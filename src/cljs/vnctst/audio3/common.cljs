(ns vnctst.audio3.common
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio3.state :as state]
            [vnctst.audio3.util :as util]
            [vnctst.audio3.device :as device]
            [cljs.core.async :as async :refer [>! <!]]
            ))

;;; ここには、SEとBGM/BGS/MEの両方ともで使う機能を入れる


(defonce background-handle (atom nil))

(defn- supervise-background! []
  (when (nil? (state/get :in-background?))
    (state/set! :in-background?
                (boolean (= js/document.visibilityState "hidden")))
    (let [event-name "visibilitychange"
          h (atom nil)]
      (reset! h (fn [e]
                  (let [bg? (boolean (= js/document.visibilityState "hidden"))]
                    (state/set! :in-background? bg?)
                    (when @background-handle
                      (@background-handle bg?)))))
      (js/document.addEventListener event-name @h))))



(defn init! [& options]
  (when-not (state/get :common-initialized?)
    (let [options (if (map? (first options))
                    (first options)
                    (apply hash-map options))
          fallback-ext (or (:fallback-ext options) "mp3")
          default-ext (if (util/can-play-ogg?) "ogg" fallback-ext)
          url-prefix (or (:url-prefix options) "audio/")
          ]
      ;; 設定項目(初回限定。後からの変更はしない想定)
      (state/set! :debug? (:debug? options))
      (state/set! :never-use-webaudio? (:never-use-webaudio? options))
      (state/set! :default-ext default-ext)
      (state/set! :url-prefix url-prefix)
      (state/set! :dont-stop-on-background? (:dont-stop-on-background? options))
      (state/set! :always-mute-at-mobile? (:always-mute-at-mobile? options))
      (state/set! :muted-by-mobile? (and
                                      (:always-mute-at-mobile? options)
                                      (:mobile util/terminal-type)))
      (state/set! :never-use-htmlaudio?
                  (and
                    (:never-use-htmlaudio-at-mobile? options)
                    (:mobile util/terminal-type)))
      (device/init! (state/get :never-use-webaudio?)
                    (state/get :never-use-htmlaudio?))
      (state/set! :common-initialized? true)
      ;; 設定項目(後で設定可能)
      ;; NB: これらの状態はinit!より前に設定されるケースがありえるので、
      ;;     便宜の為に、ここでリセットするのではなく、state側にて
      ;;     初期値として埋め込む事にし、ここではリセットしないようにする
      ;(state/set! :volume-master 0.5)
      ;(state/set! :volume-bgm 0.5)
      ;(state/set! :volume-se 0.5)
      ;; 内部状態
      (state/set! :in-background? nil)
      ;; watcher類を起動
      (when-not (:dont-stop-on-background? options)
        (supervise-background!)))))


(defn initialized? []
  (let [r (boolean (state/get :common-initialized?))]
    (when-not r
      (util/logging-force "vnctst.audio3 is not initialized !!!"))
    r))



