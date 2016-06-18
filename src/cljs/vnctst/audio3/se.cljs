(ns vnctst.audio3.se
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio3.state :as state]
            [vnctst.audio3.util :as util]
            [vnctst.audio3.device :as device]
            [vnctst.audio3.common :as common]
            [cljs.core.async :as async :refer [>! <!]]
            ))

;;; ロード済(エラー含む)のasを保持するテーブル
;;; エラー時は「エントリはあるが値はnil」となる
(defonce ^:private loaded-audiosource-table (atom {}))


;;; ロード中のasの、ロード完了時実行ハンドルを保持するテーブル
;;; (ロード完了して実行されたら消去される。完全に内部用なので公開禁止)
;;; ロード中判定にも使われる
(defonce ^:private loaded-handle-table (atom {}))


;;; 再生中のseのacをここに保存する。
;;; 適当なタイミングで定期的に検査し、終了しているacはdisposeして除外する。
;;; (つまりこれはほぼ、dispose対応の為のもの)
(defonce ^:private playing-audiochannel-pool (atom #{}))



;;; プリロードを直列処理にする為のキュー
(defonce preload-request-queue (atom nil))




;;; 以下のmsec内には、同一SEを連打する事はできない
;;; (0にすると無効化できる。また一時的に変更してもよい)
(defonce ^:dynamic same-se-interval 50)
(defonce same-se-prevent-table (atom {}))




;;; 後述のwatcherを停止させる為のchan
(defonce playing-audiochannel-pool-watcher (atom nil))

(defn- run-playing-audiochannel-pool-watcher! []
  (when-not @playing-audiochannel-pool-watcher
    (let [c (async/chan)]
      (go-loop []
        ;; TODO: cから停止コマンドを受け付けるようにする
        (<! (async/timeout 1111))
        (swap! playing-audiochannel-pool
               (fn [old]
                 (reduce (fn [stack ac]
                           (if (device/se-call! :playing? ac)
                             (conj stack ac)
                             (do
                               (device/se-call! :dispose-audio-channel! ac)
                               stack)))
                         #{}
                         old)))
        (recur))
      (reset! playing-audiochannel-pool-watcher c))))






(defn init! [& options]
  (apply common/init! options)
  (when-not (state/get :se-initialized?)
    (let [options (if (map? (first options))
                    (first options)
                    (apply hash-map options))]
      (state/set! :se-initialized? true)
      (run-playing-audiochannel-pool-watcher!))))




(defn- loading? [key-or-path]
  (or
    (contains? @loaded-handle-table key-or-path)
    (first (filter #(= key-or-path %) @preload-request-queue))))

;;; NB: エラー発生時も「ロード自体は完了」として真値を返す。
;;;     成功したかどうかを見るには succeeded-to-load? を呼ぶ事。
;;;     unload!されると偽値に戻る事にも注意。
(defn loaded? [key-or-path]
  (contains? @loaded-audiosource-table key-or-path))

;;; ロード済かつロード成功であれば真値を返す
(defn succeeded-to-load? [key-or-path]
  (boolean (get @loaded-audiosource-table key-or-path)))






;;; key-or-pathが既にロード済なら即座に、そうでなければローディングを開始し、
;;; ローディングが完了したタイミングで (done-fn) を実行する。
;;; ※done-fnは後から書き換えが行われ、実行されない場合がある。
;;; (多重に _play! リクエストが来た際に、最後のものにだけ対応する為)
;;; done-fnは「ロード成功後に再生を開始する為のもの」であるので、
;;; 「ロードの成功/失敗に関わらず、完了時に指定関数を実行してほしい」場合は
;;; done-fnを使わずに、自前で定期的に loaded? succeeded-to-load? を呼んで
;;; チェックするgoスレッドを走らせる事。
(defn- load-internal! [key-or-path & [done-fn]]
  (if (or
        (state/get :muted-by-mobile?)
        (loaded? key-or-path))
    ;; ロードが不要な場合は、即座にdone-fnを実行する
    (when done-fn
      (done-fn))
    (if (loading? key-or-path)
      ;; preload!(done-fnなし)なら何もしない。_play!ならdone-fnを差し替える
      (when done-fn
        (if (contains? @loaded-handle-table key-or-path)
          (swap! loaded-handle-table assoc key-or-path done-fn)
          ;; キュー待ちの時はどうしようもないので、ロードを開始してしまう
          ;; (このケースは稀なのでこれで問題ない筈)
          (do
            (swap! preload-request-queue
                   #(remove (fn [a] (= key-or-path a)) %))
            (load-internal! key-or-path done-fn))))
      ;; 普通にロードを開始する
      (let [url (util/key->path key-or-path)
            handle-ok (fn [as]
                        ;; NB: done-fnは後から書き換えられる可能性があるので、
                        ;;     ここで改めて最新のものを取得する
                        (let [f (get @loaded-handle-table key-or-path)]
                          (swap! loaded-audiosource-table assoc key-or-path as)
                          (swap! loaded-handle-table dissoc key-or-path)
                          (when f
                            (f))))
            handle-error (fn [msg]
                           (util/logging :error msg)
                           ;; NB: エラー時は「エントリはあるが値はnil」
                           ;;     という形式で示す。
                           ;;     うっかりここをdissocに書き換えたりしない事。
                           (swap! loaded-audiosource-table
                                  assoc key-or-path nil)
                           (swap! loaded-handle-table dissoc key-or-path))]
        (swap! loaded-handle-table assoc key-or-path done-fn)
        (device/se-call! :load-audio-source! url handle-ok handle-error)))))







(defn- played-same-se? [combined-key]
  (let [prev (or (get @same-se-prevent-table combined-key) 0)
        now (js/Date.now)
        ok? (or
              (< now prev) ; タイマー巻き戻り対策
              (< (+ prev same-se-interval) now))]
    (not ok?)))

(defn- update-played-same-se! [combined-key]
  (let [now (js/Date.now)]
    (swap! same-se-prevent-table
           (fn [old-table]
             (let [too-old (- now same-se-interval)]
               (assoc (into {} (remove (fn [[k v]]
                                         (< v too-old))
                                       old-table))
                      combined-key now))))))




(defn- _play! [key-or-path vol pitch pan alarm?]
  (when-not (state/get :muted-by-mobile?)
    (let [[i-vol i-pitch i-pan] (util/calc-internal-params :se vol pitch pan)
          combined-key [key-or-path i-vol i-pitch i-pan]]
      (when (and
              (or
                alarm?
                (not (state/get :in-background?)))
              (pos? i-vol)
              (not (played-same-se? combined-key)))
        (if-let [as (get @loaded-audiosource-table key-or-path)]
          (let [ac (device/se-call! :spawn-audio-channel as)]
            (update-played-same-se! combined-key)
            (device/se-call! :play! ac 0 false i-vol i-pitch i-pan alarm?)
            (swap! playing-audiochannel-pool conj ac)
            ;; 即値の入ったchanを生成して返す
            (async/to-chan [ac]))
          (let [ch (async/chan)]
            (load-internal! key-or-path
                            #(go
                               (let [c (_play! key-or-path
                                               vol
                                               pitch
                                               pan
                                               alarm?)
                                     ac (and c (<! c))]
                                 (if ac
                                   (async/put! ch ac)
                                   (async/close! ch)))))
            ;; ロード失敗時にchを閉じる為のgoスレッドを起動する
            ;; TODO: 現状だとrace conditionが発生し得る。
            ;;       もうちょっとなんとかしたいが…
            (go-loop []
              (<! (async/timeout 1111))
              (if-not (loaded? key-or-path)
                (recur)
                (async/close! ch)))
            ch))))))

(defn play! [key-or-path & [vol pitch pan]]
  (_play! key-or-path vol pitch pan false))

;;; play! と同じだが、バックグラウンドになっても鳴らす事のできるもの
(defn alarm! [key-or-path & [vol pitch pan]]
  (_play! key-or-path vol pitch pan true))


;;; 再生中のSEをフェードアウト停止(もしくは即時停止)させる
;;; NB: 引数は、 audio-channel の入った core.async/chan (分かりづらい)。
(defn stop! [chan & [fade-sec]]
  (when-not (state/get :muted-by-mobile?)
    (when chan
      (go
        (when-let [ac (<! chan)]
          ;; NB: acが既にdispose済の場合は何も行わないようにする必要がある。
          ;;     acがdispose済かどうかは、 playing-audiochannel-pool
          ;;     を見れば分かる。
          (when (get @playing-audiochannel-pool ac)
            (let [fade-sec (or fade-sec 0)]
              ;; TODO: フェード対応
              ;;       (今はfade-secに関わらず常に即時停止する仕様)
              (device/se-call! :stop! ac))))))))








(defonce preload-process (atom nil))

;;; seファイルをプリロードしておきたい場合はこれを呼ぶ。
;;; ロードの完了については、呼出元で適当な間隔で
;;; loaded? もしくは succeeded-to-load? を実行する事。
(defn preload! [key-or-path]
  (when (and
          (not (state/get :muted-by-mobile?))
          (not (loading? key-or-path))
          (not (loaded? key-or-path)))
    (swap! preload-request-queue #(concat % [key-or-path]))
    (util/run-preload-process! preload-process
                               preload-request-queue
                               (fn [k]
                                 (load-internal! k nil)
                                 (go-loop []
                                   (when-not (loaded? k)
                                     (<! (async/timeout 333))
                                     (recur)))))))






;;; 一時的にしか使わないseをアンロードする
;;; NB: 鳴っている最中には呼ばない事。
(defn unload! [key-or-path]
  ;; TODO: 鳴っている最中に呼ばないように、現在再生中のSEなら先に強制停止させる
  ;;       @playing-audiochannel-pool から調べて対応する
  (if (loaded? key-or-path)
    ;; ロード済
    (when-let [as (get @loaded-audiosource-table key-or-path)]
      (device/se-call! :dispose-audio-source! as)
      (swap! loaded-audiosource-table dissoc key-or-path))
    ;; ロード中でないなら、既にアンロード済の状態なので、何もする必要はない
    (when (loading? key-or-path)
      ;; ロードキュー待ち状態なら、そこから消すだけでよい
      ;; そうでないなら、ロード完了時実行関数を書き換える必要がある
      (if (contains? @loaded-handle-table key-or-path)
        (swap! loaded-handle-table assoc key-or-path #(unload! key-or-path))
        (swap! preload-request-queue
               #(remove (fn [a] (= key-or-path a)) %))))))



