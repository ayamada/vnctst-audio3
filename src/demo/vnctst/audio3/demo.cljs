(ns vnctst.audio3.demo
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [cljs.core.async :as async :refer [>! <!]]
            [vnctst.audio3 :as audio3]
            [vnctst.audio3.js :as audio3-js]))

;;; TODO: 特定のボタンを押す事によって、ボタンのラベルをcljs表記かjs表記かを変更できるようにしたい(これを行うには、ラベルおよび実行式をここのモジュールで保持する必要がある)
;;; TODO: 上記に伴い、表に表示されない「audio3/init!の実行」も表示だけ行っておきたい


(defn- display-msg! [msg & more-msgs]
  (when-let [dom (js/document.getElementById "message")]
    (set! (.. dom -textContent) (apply print-str msg more-msgs))))

(defn- display-version! []
  (when-let [dom (js/document.getElementById "version")]
    (set! (.. dom -textContent) (str "Version: "
                                     audio3-js/version))))

(defn- show-buttons! []
  (when-let [dom (js/document.getElementById "main")]
    (set! (.. dom -style -display) "block")))



(defn ^:export bootstrap []
  (audio3/init! :fallback-ext "mp3"
                :dont-stop-on-background? false
                :always-mute-at-mobile? false
                :debug? true
                :never-use-webaudio? false
                )
  ;; プリセットのプリロードとロード待ちを行う
  (let [bgm-keys (concat audio3/preset-bgm-keys
                         audio3/preset-bgs-keys
                         audio3/preset-me-keys)
        se-keys audio3/preset-se-keys
        target-num (+ (count bgm-keys)
                      (count se-keys))
        display-progress! #(display-msg! (str "Loading ... "
                                              %
                                              " / "
                                              target-num))]
    (display-version!)
    (audio3/preload-all-preset!)
    (display-progress! 0)
    (go-loop []
      (<! (async/timeout 200))
      (let [c (+ (count (filter audio3/preloaded-bgm? bgm-keys))
                 (count (filter audio3/loaded-se? se-keys)))]
        (display-progress! c)
        (if (< c target-num)
          (recur)
          (do
            (show-buttons!)
            (display-msg! "Loaded.")))))))



