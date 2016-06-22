(ns vnctst.audio3.device.web-audio
  (:require [vnctst.audio3.device.entry-table :as entry-table]
            [vnctst.audio3.util :as util]))

(defn- p [& args]
  (when entry-table/device-log-verbose?
    (util/logging :web-audio args)))








;;; 全体で使うAudioContext
(defonce audio-context (atom nil))

;;; 全体音量を変更するのに使える、が、現在は未使用
;;; (BGM系とSE系で別々にするのが困難な為)
(defonce master-gain-node (atom nil))





(defn init!? []
  (p 'init!?)
  (if @audio-context
    true
    (let [c (or
              (aget js/window "AudioContext")
              (aget js/window "webkitAudioContext"))
          ctx (when c
                (try
                  (new c)
                  (catch :default e
                    nil)))]
      (when ctx
        (util/register-ios-unlock-fn! (fn []
                                        (.start (.createBufferSource ctx) 0)
                                        true))
        (reset! audio-context ctx)
        (let [node (.createGain ctx)]
          (set! (.. node -gain -value) 1)
          (.connect node (.-destination ctx))
          (reset! master-gain-node node))
        true))))






(defn load-audio-source! [url loaded-handle error-handle]
  (p 'load-audio-source! url)
  (let [xhr (js/XMLHttpRequest.)
        h (fn [e]
            (if-not (= "2" (first (str (.. e -target -status))))
              (error-handle (str "cannot load url " url))
              (let [h2 (fn [buf]
                         (if-not buf
                           (error-handle (str "cannot decode url " url))
                           (loaded-handle {:type :audio-source
                                           :url url
                                           :buffer buf
                                           :duration (.-duration buf)
                                           })))
                    eh2 (fn [& _]
                          (error-handle (str "cannot decode url " url)))]
                (.decodeAudioData @audio-context (.-response xhr) h2 eh2))))
        eh (fn [e]
             (error-handle (str "cannot load url " url)))]
    (.open xhr "GET" url)
    (set! (.-responseType xhr) "arraybuffer")
    (set! (.-onload xhr) h)
    (set! (.-onerror xhr) eh)
    (.send xhr)))





(defn dispose-audio-source! [audio-source]
  (p 'dispose-audio-source! (:url audio-source))
  ;; :web-audio では、全てが自動でGC可能との事
  nil)





(defn spawn-audio-channel [audio-source]
  (p 'spawn-audio-channel (:url audio-source))
  ;; NB: :web-audio では、 :audio-buffer-source-node は一度しか .play する事が
  ;;     できない為、 audio-channelの実インスタンスとしては不適切。なので、
  ;;     ここでは「:audio-buffer-source-nodeを生成する際に必要なmutable情報を
  ;;     保持したatom」として、audio-channelを生成する。
  (atom (merge audio-source
               {:type :audio-channel
                :audio-source audio-source
                :audio-buffer-source-node nil
                :vol 1
                :pitch nil
                :pan 0.5
                :loop nil
                })))





(defn- update-panner-node! [panner-node pan]
  (if (number? pan)
    (let [x pan
          y 0
          z (- 1 (js/Math.abs x))]
      (.setPosition panner-node x y z))
    (let [[x y z] (seq pan)]
      (.setPosition panner-node x y z))))



(defn _pos [ch]
  (if-let [play-start-time (:play-start-time @ch)]
    (let [play-end-time (or
                          (:play-end-time @ch)
                          (.-currentTime @audio-context))
          ;; NB: posは実時間ではなくpitch=1の時の秒数を返す必要があるので、
          ;;     ここで変換する必要がある
          pitch (:pitch @ch)
          started-pos (:started-pos @ch)
          duration (:duration @ch)
          playtime (- play-end-time play-start-time)
          normalized-playtime (* playtime pitch)
          pos-tmp (+ started-pos normalized-playtime)]
      (loop [p pos-tmp]
        (if (<= duration p)
          (recur (- p duration))
          p)))
    0))

(defn pos [ch]
  (p 'pos (:url @ch))
  (_pos ch))

(defn- safe-disconnect! [node]
  (when node
    (try
      (.disconnect node)
      (catch :default e
        nil))))



(defn play! [ch start-pos loop? volume pitch pan alarm?]
  (p 'play! (:url @ch) start-pos loop? volume pitch pan alarm?)
  ;; NB: 論理層にて、再生中にもう一度再生が来る事はない事が保証されている
  ;;     (論理層側で、適切にフェードやstop!等が間に挟まれる)
  (let [buf (:buffer @ch)
        source-node (.createBufferSource @audio-context)
        gain-node (.createGain @audio-context)
        ;; NB: ここを .createStereoPanner にする案があったが、
        ;;     webkit系での対応状況が悪いので却下された。
        ;;     将来に対応が進んだら、再度検討してもよい
        panner-node (.createPanner @audio-context)]
    (set! (.-buffer source-node) buf)
    (set! (.. source-node -playbackRate -value) pitch)
    (set! (.. gain-node -gain -value) volume)
    (set! (.-panningModel panner-node) "equalpower")
    (update-panner-node! panner-node pan)
    (.connect source-node gain-node)
    (.connect gain-node panner-node)
    ;; alarm?が真の時は、master-gain-nodeを通さないようにしてみる(仮)
    ;; NB: バックグラウンド時の消音を、デバイス側で master-gain-node
    ;;     を使って行う場合にこの処理が必要になるが、バックグラウンド時の消音は
    ;;     今のところデバイス層ではなく内部層で対応する予定なので、
    ;;     この処理は不要な筈。ただ入れておいても動作に不具合が出る類の
    ;;     コードではないので、とりあえず入れておく。
    ;;     (現状ではmaster-gain-nodeを活用していないので、差が出ない為)
    (if alarm?
      (.connect panner-node (.-destination @audio-context))
      (.connect panner-node @master-gain-node))
    (when loop?
      ;; TODO: 将来にループポイントを個別に設定できるようにする
      (set! (.-loop source-node) true)
      (set! (.-loopStart source-node) 0)
      (set! (.-loopEnd source-node) (:duration @ch)))
    (aset source-node
          "onended"
          #(let [now (.-currentTime @audio-context)]
             (safe-disconnect! source-node)
             (safe-disconnect! gain-node)
             (safe-disconnect! panner-node)
             (swap! ch assoc
                    :audio-buffer-source-node nil
                    :gain-node nil
                    :panner-node nil
                    :play-end-time now)))
    (let [now (.-currentTime @audio-context)]
      (swap! ch merge {:audio-buffer-source-node source-node
                       :gain-node gain-node
                       :panner-node panner-node
                       :vol volume
                       :pitch pitch
                       :pan pan
                       :loop loop?
                       :started-pos start-pos
                       :play-start-time now
                       :play-end-time nil})
      (.start source-node now start-pos)
      ch)))

(defn playing? [ch]
  (p 'playing? (:url @ch))
  (and
    ;; :play-start-time がない場合、初回再生前なので停止中
    (:play-start-time @ch)
    ;; :play-start-time があり、:play-end-timeがある場合、停止中
    ;; :play-start-time があり、:play-end-timeがない場合、再生中
    (not (:play-end-time @ch))))

(defn stop! [ch]
  (p 'stop! (:url @ch))
  ;; NB: race conditionがありえるので、tryで囲む
  (try
    (.stop (:audio-buffer-source-node @ch))
    ;; NB: :play-end-timeへの反映は、 onended ハンドルで行われる想定
    (catch :default e
      nil)))

(defn set-volume! [ch volume]
  (p 'set-volume! (:url @ch) volume)
  ;; NB: race conditionがありえるので、tryで囲む
  (try
    (when-let [node (:gain-node @ch)]
      (set! (.. node -gain -value) volume)
      (swap! ch assoc :vol volume))
    (catch :default e
      nil)))

(defn set-pitch! [ch pitch]
  (p 'set-pitch! (:url @ch) pitch)
  ;; NB: race conditionがありえるので、tryで囲む
  (try
    (when-let [node (:audio-buffer-source-node @ch)]
      (let [now (.-currentTime @audio-context)
            current-pos (_pos ch)]
        (set! (.. node -playbackRate -value) pitch)
        ;; 計算が面倒なので、「このタイミングから再生を開始した」という形に
        ;; パラメータを書き換える事での対応とする
        ;; (この情報が使われるのは pos の算出のみなので、精度は不要)
        (swap! ch merge {:pitch pitch
                         :started-pos current-pos
                         :play-start-time now
                         })))
    (catch :default e
      nil)))

(defn set-pan! [ch pan]
  (p 'set-pan! (:url @ch) pan)
  ;; NB: race conditionがありえるので、tryで囲む
  (try
    (when-let [node (:panner-node @ch)]
      (update-panner-node! node pan)
      (swap! ch assoc :pan pan))
    (catch :default e
      nil)))

(defn dispose-audio-channel! [ch]
  (p 'dispose-audio-channel! (:url @ch))
  ;; :web-audio では、全てが自動でGC可能との事
  nil)






(entry-table/register!
  :web-audio
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


