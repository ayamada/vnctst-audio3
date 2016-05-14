(ns vnctst.audio3.device.html-audio-single
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio3.device.entry-table :as entry-table]
            [vnctst.audio3.util :as util]
            [cljs.core.async :as async :refer [>! <!]]
            ))

(def ^:dynamic p-key :html-audio-single)

(defn- p [& args]
  (util/logging p-key args))


;;; 実装について
;;; - html-audio-single は「単一再生のみでよい」ので、
;;;   audio-source = audio-channel という構造とし、
;;;   audio-source 側に再生インスタンスの実体を持たせる形式とした。






;;; 頻出するaudio-classをここに保持しておく
(defonce audio-class (atom nil))
(defn new-audio [url]
  (let [ac @audio-class]
    (new ac url)))


(defonce first-audio-url-for-ios-unlock (atom nil))





(defn init!? []
  (p 'init!?)
  (if @audio-class
    true
    (let [ac (or
               (aget js/window "Audio")
               (aget js/window "webkitAudio"))
          audio (when ac
                  (try
                    (new ac "")
                    (catch :default e
                      nil)))]
      (when audio
        (reset! audio-class ac)
        (util/register-ios-unlock-fn!
          (fn []
            ;; NB: :html-audioでのアンロックには音源ファイルが必須なので、
            ;;     何かしらプリロードされるまではアンロックできない
            (when-let [url @first-audio-url-for-ios-unlock]
              (let [a (new-audio url)]
                (set! (.-volume a) 0)
                (set! (.-loop a) false)
                (.play a)
                true))))
        true))))





;;; NB: 現状では、audio-source = audio-channel の扱い
(defn _load-audio-source! [url loaded-handle error-handle]
  ;; NB: よく分からないが、ieのみ、 .addEventListener で各種イベントを
  ;;     捕捉できない時があるので、イベントが捕捉できなくても
  ;;     ロード完了を検知する必要がある
  (let [a (new-audio url)
        h-loaded (atom nil)
        h-error (atom nil)
        h-ended (atom nil)
        as {:url url
            :audio a
            :error? (atom false)
            :loaded? (atom false)
            :playing-info (atom nil)
            :play-request (atom nil)
            }
        handle-keys ["loadeddata"
                     "canplay"
                     ;; 環境によっては、以下での判定になるものがあるらしい、
                     ;; しかしこれらを設定すると他の環境で誤判定になるので、
                     ;; これらについては諦めてタイマーでの検知とする
                     ;"suspend"
                     ;"stalled"
                     ]]
    (reset! h-loaded (fn [e]
                       (when-not @(:loaded? as)
                         (reset! (:loaded? as) true)
                         (doseq [k handle-keys]
                           (.removeEventListener a k @h-loaded))
                         (when-not @first-audio-url-for-ios-unlock
                           (reset! first-audio-url-for-ios-unlock url))
                         (loaded-handle as))))
    (doseq [k handle-keys]
      (.addEventListener a k @h-loaded))
    (reset! h-error (fn [e]
                      (when-not @(:loaded? as)
                        (.removeEventListener a "error" @h-error)
                        (reset! (:loaded? as) true)
                        (reset! (:error? as) true)
                        (error-handle (str "cannot load url " url)))))
    (.addEventListener a "error" @h-error)
    (reset! h-ended (fn [e]
                      (reset! (:playing-info as) nil)))
    (.addEventListener a "ended" @h-ended)
    (set! (.-preload a) "auto")
    (set! (.-autoplay a) false)
    (set! (.-muted a) false)
    (set! (.-controls a) false)
    (.load a)
    (go-loop [elapsed-sec 0]
      (<! (async/timeout 1000))
      (when-not @(:loaded? as)
        (if (= 4 (.-readyState a))
          (@h-loaded nil)
          (if (< 30 elapsed-sec)
            (do
              (reset! (:loaded? as) true)
              (reset! (:error? as) true)
              (error-handle (str "timeout to load url " url)))
            (recur (inc elapsed-sec))))))
    as))

(defn load-audio-source! [url loaded-handle error-handle]
  (p 'load-audio-source! url)
  (_load-audio-source! url loaded-handle error-handle))




(defn dispose-audio-source! [audio-source]
  (p 'dispose-audio-source! (:url audio-source))
  nil)




(defn spawn-audio-channel [audio-source]
  (p 'spawn-audio-channel (:url audio-source))
  (atom (merge audio-source
               {:type :audio-channel
                :audio-source audio-source
                })))






(defn playing? [ch]
  (p 'playing? (:url @ch))
  (when-let [playing-info @(:playing-info @ch)]
    (not (:end-msec playing-info))))



(defn pos [ch]
  (p 'pos (:url @ch))
  (or
    (try
      (when (playing? ch)
        (.-currentTime (:audio @ch)))
      (catch :default e nil))
    (let [playing-info @(:playing-info @ch)
          offset-msec (* (:start-pos playing-info) 1000)
          begin-msec (:begin-msec playing-info)
          end-msec (or (:end-msec playing-info) (js/Date.now))]
      (max 0 (/ (+ offset-msec (- end-msec begin-msec)) 1000)))))


(defn- _set-pitch! [audio pitch]
  ;; 試してみたが、ブラウザ側の実装が悪いようで、音程が変化せずに
  ;; 再生が途切れたりするようになるだけなので、無効化する事にした
  ;(try
  ;  (set! (.-playbackRate audio pitch)
  ;  (catch :default e nil))
  nil)

(defn play! [ch start-pos loop? volume pitch pan alarm?]
  (p 'play! (:url @ch) start-pos loop? volume pitch pan alarm?)
  (when-not @(:error? @ch)
    ;; NB: html-audio-multiからの再生時に、
    ;;     実際のロード生成が遅延するケースがある。
    ;;     これにきちんと対応できなくてはならない。
    ;;     (以下の .-readyState が4以外だった時の処理)
    (let [a (:audio @ch)]
      (if (= 4 (.-readyState a))
        (do
          ;(.pause a)
          (set! (.-loop a) (boolean loop?))
          (set! (.-volume a) volume)
          ;; NB: これが上手く機能しない(=常に0扱いになる)環境があるようだ。
          ;;     しかしこれが必要になるのはbackgroundからの復帰時のみで、
          ;;     その場合は曲の最初から再生し直しても大きな問題はないので、
          ;;     上手く動かない環境でも、現状ではこれでよいという事にする。
          ;;     (将来に「途中ポイントからの再生」を外部に提供しようと
          ;;     考えた場合には問題になるので注意)
          (set! (.-currentTime a) start-pos)
          (_set-pitch! a pitch)
          (reset! (:playing-info @ch) {:start-pos start-pos
                                       :begin-msec (js/Date.now)
                                       :end-msec nil
                                       })
          ;; 非ループ時は、再生終了時に状態を変更するgoスレッドを起動する
          (when-not loop?
            (go-loop []
              (<! (async/timeout 888))
              ;; dispose-audio-channel!されたら終了
              (when-let [playing-info @(:playing-info @ch)]
                ;; stop!されたら終了
                (when-not (:end-msec playing-info)
                  (if (.-ended (:audio @ch))
                    (swap! (:playing-info @ch) assoc :end-msec (js/Date.now))
                    (recur))))))
          (.play a))
        (do
          (reset! (:play-request @ch)
                  [start-pos loop? volume pitch pan alarm?])
          (go-loop []
            (<! (async/timeout 1))
            (when-let [play-request @(:play-request @ch)]
              (if (= 4 (.-readyState a))
                (do
                  (reset! (:play-request @ch) nil)
                  (apply play! ch play-request))
                (recur)))))))))



(defn stop! [ch]
  (p 'stop! (:url @ch))
  (.pause (:audio @ch))
  ;; NB: :play-request のキャンセルが必須
  (reset! (:play-request @ch) nil)
  (swap! (:playing-info @ch) assoc :end-msec (js/Date.now)))

(defn set-volume! [ch volume]
  (p 'set-volume! (:url @ch) volume)
  (set! (.-volume (:audio @ch)) volume))

(defn set-pitch! [ch pitch]
  (p 'set-pitch! (:url @ch) pitch)
  (_set-pitch! (:audio @ch) pitch)
  nil)

(defn set-pan! [ch pan]
  (p 'set-pan! (:url @ch) pan)
  ;; NB: :html-audio は pan 非対応
  nil)

(defn dispose-audio-channel! [ch]
  (p 'dispose-audio-channel! (:url @ch))
  (reset! (:playing-info @ch) nil)
  nil)






(entry-table/register!
  :html-audio-single
  {:init!? init!?
   :load-audio-source! load-audio-source!
   :dispose-audio-source! dispose-audio-source!
   :spawn-audio-channel spawn-audio-channel
   :pos pos
   :play! play!
   :playing? playing?
   :stop! stop!
   :set-volume! set-volume!
   :set-pitch! set-pitch!
   :set-pan! set-pan!
   :dispose-audio-channel! dispose-audio-channel!
   })


