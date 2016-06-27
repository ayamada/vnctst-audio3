(ns vnctst.audio3.bgm
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio3.state :as state]
            [vnctst.audio3.util :as util]
            [vnctst.audio3.device :as device]
            [vnctst.audio3.common :as common]
            [cljs.core.async :as async :refer [>! <!]]
            ))




(defonce preload-process (atom nil))
(defonce preload-request-queue (atom nil))
(defonce preloaded-handle-table (atom {}))

;;; key-or-path => audio-source
(defonce preload-table (atom {}))


;;; NB: 「プリロード最中」「まだ待ちキューに入ってるだけ」の両方で真を返す
(defn preloading? [key-or-path]
  (or
    (contains? @preloaded-handle-table key-or-path)
    (boolean (first (filter #{key-or-path} @preload-request-queue)))))


(defn preloaded? [key-or-path]
  (contains? @preload-table key-or-path))

(defn succeeded-to-preload? [key-or-path]
  (boolean (get @preload-table key-or-path)))


;;; TODO: これを外からいじるインターフェースを提供する事
(defonce ^:dynamic default-fade-sec 1.0)

;;;フェード時の音量変更の単位粒度(msec)
(defonce ^:dynamic fade-granularity-msec 100)







;;; bgm-me-state bgs-state には、 nil もしくは以下のkeyを持つmapが入る
;;; - :fade-factor ; 通常は 1.0、フェード中のみ変化する
;;; - :fade-delta ; 通常は0。基本となるフェード量が設定される
;;; - :fade-process ; フェードを進めるgoスレッド、通常はnil
;;; - :current-param ; :key :volume :pan :pitch :loop? :as :ac を持つ map
;;; - :next-param ; 「次の曲」。内容は :current-param と同様。「ロード中」を
;;;   示すのもこれで表現される

(defonce bgm-me-state (atom nil))
(defonce bgs-state (atom nil))


;;; 基本的に、stateのある時は再生中相当。
;;; (フェード中や再生前ロード中も「再生中」に相当させたい為)
(defn- _playing-bgm? [state]
  (when-let [ac (:ac (:current-param @state))]
    ;; NB: MEのみ、acが存在して再生終了状態になっているケースがあるので、
    ;;     きちんと調べる
    (boolean (device/bgm-call! :playing? ac))))
(defn playing-bgm? [] (_playing-bgm? bgm-me-state))
(defn playing-bgs? [] (_playing-bgm? bgs-state))
(defn playing-me? [] (_playing-bgm? bgm-me-state))



;;; バックグラウンド復帰の再生ポイント記録用
(defonce bgm-resume-pos (atom nil))
(defonce bgs-resume-pos (atom nil))



(defn- fading? [state]
  (when-let [fade-delta (:fade-delta @state)]
    (not (zero? fade-delta))))

(defn- fade-out? [state]
  (and
    (fading? state)
    (neg? (:fade-delta @state))))

(defn- fade-in? [state]
  (and
    (fading? state)
    (pos? (:fade-delta @state))))



;;; 更新されたvolume値およびfade状態を、device側に反映させる
(defn- sync-volume! [state]
  (when-not (state/get :in-background?)
    (when-let [param (:current-param @state)]
      (when-let [ac (:ac param)]
        (when-let [vol (:volume param)]
          (let [[i-vol _ _] (util/calc-internal-params :bgm vol)
                i-vol (* i-vol (:fade-factor @state))]
            (device/bgm-call! :set-volume! ac i-vol)))))))

(defn sync-bgm-volume! []
  (sync-volume! bgm-me-state))

(defn sync-bgs-volume! []
  (sync-volume! bgs-state))




;;; NB: 分かりづらいが、これはプリロード専用。プリロードを使わない再生用の
;;;     ロードについては、 _load+play! を参照
(defn- load-internal! [key-or-path & [done-fn]]
  (if (or
        (state/get :muted-by-mobile?)
        (preloaded? key-or-path))
    ;; ロードが不要な場合は、即座にdone-fnを実行する
    (when done-fn
      (done-fn))
    (if (preloading? key-or-path)
      ;; preload!(done-fnなし)なら何もしない。_play!ならdone-fnを差し替える
      (when done-fn
        (if (contains? @preloaded-handle-table key-or-path)
          (swap! preloaded-handle-table assoc key-or-path done-fn)
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
                        (let [f (get @preloaded-handle-table key-or-path)]
                          (swap! preload-table assoc key-or-path as)
                          (swap! preloaded-handle-table dissoc key-or-path)
                          (when f
                            (f))))
            handle-error (fn [msg]
                           (util/logging :error msg)
                           ;; NB: エラー時は「エントリはあるが値はnil」
                           ;;     という形式で示す。
                           ;;     うっかりここをdissocに書き換えたりしない事。
                           (swap! preload-table assoc key-or-path nil)
                           (swap! preloaded-handle-table dissoc key-or-path))]
        (swap! preloaded-handle-table assoc key-or-path done-fn)
        (device/bgm-call! :load-audio-source! url handle-ok handle-error)))))



(defn- dispose-as! [as & [dispose-from-pool?]]
  (when as
    (let [pooled-key (ffirst (filter (fn [[k v]] (= v as))
                                     @preload-table))]
      (if dispose-from-pool?
        ;; poolから削除する。poolにない場合もdisposeは必ず行う
        (do
          (when pooled-key
            (swap! preload-table dissoc pooled-key))
          (device/bgm-call! :dispose-audio-source! as))
        ;; poolにない時のみdisposeを行う
        (when-not pooled-key
          (device/bgm-call! :dispose-audio-source! as))))))

(defn- dispose-ac! [ac]
  (when ac
    (device/bgm-call! :dispose-audio-channel! ac)))






(defn- _play-immediately! [mode state as param]
  (let [ac (device/bgm-call! :spawn-audio-channel as)
        vol (:volume param)
        pitch (:pitch param)
        pan (:pan param)
        loop? (:loop? param)
        [i-vol i-pitch i-pan] (util/calc-internal-params :bgm vol pitch pan)
        new-param (assoc param
                         :as as
                         :ac ac)]
    (swap! state merge {:fade-factor 1
                        :fade-delta 0
                        :fade-process nil
                        :current-param new-param
                        :next-param nil})
    ;; バックグラウンド時は再生しないようにする。
    ;; また、バックグラウンド時は明示的な再開ポイントのリセットが必要
    ;; (非バックグラウンド時はイベントリスナで設定されるが、そうではないので)
    (if (state/get :in-background?)
      (let [a (if (= :bgs mode)
                bgs-resume-pos
                bgm-resume-pos)]
        (reset! a 0))
      (device/bgm-call! :play! ac 0 loop? i-vol i-pitch i-pan false))))



;;; NB: ロード中にキャンセルされたり、別の音源の再生要求が入ったりする。
;;;     その時はどちらの場合もロード完了後に :dispose-audio-source! する。
(defn- _load+play! [mode state key-or-path vol pitch pan loop?]
  (let [url (util/key->path key-or-path)
        previous-loading-key (:key (:next-param @state))
        h-done (fn [as]
                 (or
                   ;; ロードがキャンセルされていないかチェック
                   (when-let [next-param (:next-param @state)]
                     (when (= key-or-path (:key next-param))
                       ;; キャンセルされていない。再生を実行
                       (_play-immediately! mode state as next-param)
                       true))
                   ;; ロードがキャンセルされていた。破棄する
                   (dispose-as! as)))
        h-err (fn [msg]
                (util/logging :error msg)
                (when @state
                  (swap! state assoc :next-param nil)))
        param {:key key-or-path
               :volume vol
               :pan pan
               :pitch pitch
               :loop? loop?
               }]
    ;; とりあえず再生対象を :next-param として積んでおく
    (swap! state assoc :next-param param)
    (if (preloaded? key-or-path)
      ;; プリロード済なら、それを利用する
      (if-let [as (get @preload-table key-or-path)]
        (h-done as)
        (h-err "load error"))
      (let [;; プリロードに依存せず、自力でロードを行う場合の処理。
            ;; ロード対象の上書き時にロード対象が以前と同一だった場合は
            ;; goスレッドを再利用できるので、
            ;; 新しく :load-audio-source! を実行しなくてすむ。
            ;; (先に行われている :next-param の更新だけでok)
            start! #(when-not (= previous-loading-key key-or-path)
                      (device/bgm-call! :load-audio-source! url h-done h-err))]
        (if-not (preloading? key-or-path)
          ;; プリロード対象ではない。普通に処理する
          (start!)
          ;; プリロード中。特殊な処理が必要
          (if-not (contains? @preloaded-handle-table key-or-path)
            ;; プリロード対象がロード中でない(待ちキューにある)場合は、
            ;; 待たずに普通に処理する
            ;; (本当は割り込み優先ロードさせたいが、実装が困難なので、
            ;; 優先させる為に、このようにする事を許容した)
            (start!)
            ;; プリロード中の場合は、プリロードが完了してから処理を行う
            (go-loop []
              (<! (async/timeout 555))
              (if (contains? @preloaded-handle-table key-or-path)
                (recur)
                (if-let [as (get @preload-table key-or-path)]
                  (h-done as)
                  (h-err "load error"))))))))))




(defn- _stop-immediately! [state]
  (when @state
    (let [param (:current-param @state)]
      (when-let [ac (:ac param)]
        ;; バックグラウンド中は既に再生停止している。
        ;; 二重に停止させないようにする
        (when-not (state/get :in-background?)
          (device/bgm-call! :stop! ac))
        (dispose-ac! ac))
      (dispose-as! (:as param))
      (reset! state nil))))



(defn- run-fader! [mode state]
  (when-not (:fade-process @state)
    (let [interval-msec fade-granularity-msec
          c (async/chan)]
      (go-loop []
        (<! (async/timeout interval-msec))
        (if (state/get :in-background?)
          (recur) ; バックグラウンド時はフェード処理を進めないようにする
          (when @state
            (let [delta (:fade-delta @state)
                  new-factor (max 0 (min 1 (+ delta (:fade-factor @state))))
                  end-value (if (pos? delta) 1 0)]
              (when-not (zero? delta)
                (swap! state assoc :fade-factor new-factor)
                (sync-volume! state)
                (if (and
                      (not= end-value new-factor)
                      ;; NB: 以下の判定を行うかどうかは迷う。
                      ;;     この判定は「MEのフェード中にMEの再生が完了した」
                      ;;     場合の為のものなのだが、
                      ;;     このチェックがなくても「フェードの残り秒数分だけ
                      ;;     無駄に待たされる」程度の問題しかない。
                      ;;     なので、この判定がそこそこ重いようであれば、
                      ;;     この判定は行わない方がよい。
                      ;(when-let [ac (:ac (:current-param @state))]
                      ;  (device/bgm-call! :playing? ac))
                      )
                  (recur)
                  (do
                    (swap! state assoc :fade-process nil)
                    ;; フェードアウト完了時のみ、
                    ;; 次の曲が指定されているなら対応する必要がある
                    (when (zero? end-value)
                      (let [next-param (:next-param @state)]
                        (_stop-immediately! state)
                        (when next-param
                          (_load+play! mode
                                       state
                                       (:key next-param)
                                       (:volume next-param)
                                       (:pitch next-param)
                                       (:pan next-param)
                                       (:loop? next-param)
                                       )))))))))))
      (swap! state assoc :fade-process c))))





(defn- apply-parameter! [state vol pitch pan]
  (let [[_ i-pitch i-pan] (util/calc-internal-params :bgm vol pitch pan)
        param (assoc (:current-param @state)
                     :volume vol
                     :pan pan
                     :pitch pitch)]
    (swap! state assoc :current-param param)
    (when-let [ac (:ac param)]
      (when-not (state/get :in-background?)
        (device/bgm-call! :set-pitch! ac i-pitch)
        (device/bgm-call! :set-pan! ac i-pan)
        (sync-volume! state)))))



(defn- same-bgm? [state key-or-path vol pitch pan]
  (when-let [param (:current-param @state)]
    (and
      (= (:key param) key-or-path)
      (= (:pitch param) pitch)
      true)))

;;; device側での再生状態をこちら側にも反映する
;;; (非ループ指定の時に、勝手に終わる為。逆はないので気にしなくてよい)
(defn- sync-playing-state! [state]
  (when-let [param (:current-param @state)]
    (when-let [ac (:ac param)]
      (when-not (device/bgm-call! :playing? ac)
        (_stop-immediately! state)))))

;;; 再生への状態遷移については、以下のパターンがある
;;; - 停止 → 再生開始(未ロード時の対応含む)
;;; - プリロード中 → 上記の「未ロード時の対応」と合流
;;; - ロード中 → ロード対象を差し替え
;;; - 再生中orフェードイン中 → 同一音源なので何もしない
;;; - 再生中orフェードイン中 → フェードアウト → 指定音源を再生開始
;;; - フェードアウト中 → 同一音源なのでフェードインに変更
;;; - フェードアウト中 → フェードアウト完了後に指定音源を再生開始

(defn- _play! [mode key-or-path vol pitch pan loop?]
  ;; まず、modeによって、各種のターゲットを絞る
  (let [vol (or vol 1)
        pitch (or pitch 1)
        pan (or pan 0)
        state (if (= :bgs mode)
                       bgs-state
                       bgm-me-state)]
    (sync-playing-state! state)
    ;; NB: これが呼ばれたタイミングで、どのパターンであっても、
    ;;     とりあえず以前の :next-param は無用になるので消しておく
    (when @state
      (swap! state assoc :next-param nil))
    (cond
      ;; 停止中(もしくはプリロード中)なら、即座に再生を開始するだけでよい
      (not @state)
      (_load+play! mode state key-or-path vol pitch pan loop?)
      ;; ロード中(再生準備中)なら、ロード対象を差し替える
      (and
        (not (fading? state))
        (:next-param @state))
      (_load+play! mode state key-or-path vol pitch pan loop?)
      ;; 同一BGMの場合、パラメータの変更だけで済ませる
      ;; (ただしフェード中のみ特殊処理が必要)
      (same-bgm? state key-or-path vol pitch pan)
      (do
        (when (fading? state)
          (let [fade-msec (int (* 1000 (or default-fade-sec 0)))
                fade-delta (/ fade-granularity-msec fade-msec)]
            (swap! state merge {:fade-delta fade-delta
                                :next-param nil})))
        (apply-parameter! state vol pitch pan))
      ;; そうでない場合は、フェードアウトさせてから再生する
      :else (let [fade-msec (int (* 1000 (or default-fade-sec 0)))
                  fade-delta (- (/ fade-granularity-msec fade-msec))
                  next-param {:key key-or-path
                              :volume vol
                              :pan pan
                              :pitch pitch
                              :loop? loop?}]
              ;; NB: 既にフェード中の場合の為に、 :fade-factor はいじらない
              (swap! state merge {:fade-delta fade-delta
                                  :next-param next-param})
              (run-fader! mode state)))))





(defn play-bgm! [key-or-path vol pitch pan]
  (_play! :bgm key-or-path vol pitch pan true)
  true)

(defn play-bgs! [key-or-path vol pitch pan]
  (_play! :bgs key-or-path vol pitch pan true)
  true)

(defn play-me! [key-or-path vol pitch pan]
  (_play! :me key-or-path vol pitch pan false)
  true)










;;; 停止への状態遷移については、以下のパターンがある
;;; - 停止 → 何もしない
;;; - プリロード中 → 何もしない(プリロード自体は、再生イベントとは直交)
;;; - ロード中 → ロードをキャンセル
;;;   (この「ロード中」には「プリロード中」は含まない事に要注意)
;;; - 再生中orフェードイン中 → フェードアウト開始(fade-secが0なら即座に停止)
;;; - フェードアウト中 → 何もしない(fade-secが0なら即座に停止)

(defn- _stop! [mode fade-sec]
  (let [state (if (= :bgs mode)
                       bgs-state
                       bgm-me-state)
        fade-msec (int (* 1000 (or fade-sec default-fade-sec 0)))]
    (sync-playing-state! state)
    (when @state
      (if (zero? fade-msec)
        (_stop-immediately! state)
        (let [fade-delta (- (/ fade-granularity-msec fade-msec))]
          ;; NB: 既にフェード中の場合の対応の為に、 :fade-factor はいじらない
          (swap! state merge {:fade-delta fade-delta
                              ;; ロードをキャンセルする
                              :next-param nil})
          (run-fader! mode state)))
      true)))



(defn stop-bgm! [& [fade-sec]] (_stop! :bgm fade-sec))
(defn stop-bgs! [& [fade-sec]] (_stop! :bgs fade-sec))
(defn stop-me! [& [fade-sec]] (_stop! :me fade-sec))







;;; バックグラウンドに入ったので、stateの再生を停止する
(defn- background-on! [k state pos]
  ;; acが存在する場合は基本的には論理再生中。
  ;; ただし、ME再生完了の場合はその限りではなく、個別に対応する必要がある
  (when-let [ac (:ac (:current-param @state))]
    ;; NB: MEのみ、acが存在して再生終了状態になっているケースがある
    (when (device/bgm-call! :playing? ac)
      (reset! pos (device/bgm-call! :pos ac))
      (device/bgm-call! :stop! ac))))

;;; バックグラウンドが解除されたので、復帰させるべき曲があれば、再生を再開する
(defn- background-off! [k state pos]
  (when @pos
    (let [param (:current-param @state)]
      (when-let [ac (:ac param)]
        (let [vol (:volume param)
              pitch (:pitch param)
              pan (:pan param)
              loop? (:loop? param)
              [i-vol i-pitch i-pan] (util/calc-internal-params
                                      :bgm vol pitch pan)
              i-vol (* i-vol (:fade-factor @state))]
          (device/bgm-call! :play! ac @pos loop? i-vol i-pitch i-pan false)
          (reset! pos nil))))))

(defn- sync-background! [bg?]
  (doseq [[k state pos] [[:bgm-or-me bgm-me-state bgm-resume-pos]
                         [:bgs bgs-state bgs-resume-pos]]]
    (if bg?
      (background-on! k state pos)
      (background-off! k state pos))))






(defn init! [& options]
  (apply common/init! options)
  (when-not (state/get :bgm-initialized?)
    (let [options (if (map? (first options))
                    (first options)
                    (apply hash-map options))]
      (reset! common/background-handle sync-background!)
      (state/set! :bgm-initialized? true))))










;;; ロードの完了判定については、呼出元で適当な間隔で
;;; preloaded? もしくは succeeded-to-preload? を実行する事。
(defn preload! [key-or-path]
  (when (and
          (not (state/get :muted-by-mobile?))
          (not (preloading? key-or-path))
          (not (preloaded? key-or-path)))
    (swap! preload-request-queue #(concat % [key-or-path]))
    (util/run-preload-process! preload-process
                               preload-request-queue
                               (fn [k]
                                 (load-internal! k nil)
                                 (go-loop []
                                   (when-not (preloaded? k)
                                     (<! (async/timeout 333))
                                     (recur)))))))







(defn unload! [key-or-path]
  ;; 鳴っている最中に呼ばないように、現在再生中の曲なら先に強制停止させる
  (when (= key-or-path (get-in @bgm-me-state [:current-param :key]))
    (stop-bgm! key-or-path 0))
  (when (= key-or-path (get-in @bgs-state [:current-param :key]))
    (stop-bgs! key-or-path 0))
  (if (preloaded? key-or-path)
    ;; ロード済
    (when-let [as (get @preload-table key-or-path)]
      (dispose-as! as true)
      (swap! preload-table dissoc key-or-path))
    ;; ロード中でないなら、既にアンロード済の状態なので、何もする必要はない
    (when (preloading? key-or-path)
      ;; ロードキュー待ち状態なら、そこから消すだけでよい
      ;; そうでないなら、ロード完了時実行関数を書き換える必要がある
      (if (contains? @preloaded-handle-table key-or-path)
        (swap! preloaded-handle-table assoc key-or-path #(unload! key-or-path))
        (swap! preload-request-queue
               #(remove (fn [a] (= key-or-path a)) %))))))


