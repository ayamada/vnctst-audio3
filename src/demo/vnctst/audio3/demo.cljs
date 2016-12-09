(ns vnctst.audio3.demo
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [cljs.core.async :as async :refer [>! <!]]
            [clojure.string :as string]
            [vnctst.audio3 :as audio3]
            [vnctst.audio3.js :as audio3-js]))



(defonce display-js-mode? (atom false))

(def init-options
  [:fallback-ext "mp3"
   :dont-stop-on-background? false
   :always-mute-at-mobile? false
   :debug? true
   :never-use-webaudio? false
   ])

(def button-assign
  {
   ;; BGM / ME
   :stop-bgm {:fn #(vnctst.audio3/stop-bgm!)
              :cljs "(vnctst.audio3/stop-bgm!)"
              :js "vnstst.audio3.js.stopBgm()"
              :desc "BGM / ME をフェード停止させる"
              }
   :stop-bgm-0 {:fn #(vnctst.audio3/stop-bgm! 0)
                :cljs "(vnctst.audio3/stop-bgm! 0)"
                :js "vnstst.audio3.js.stopBgm(0)"
                :desc "BGM / ME を即座に停止させる(引数はフェード秒数)"
                }
   :play-bgm-va3 {:fn #(vnctst.audio3/play! :bgm/va3)
                  :cljs "(vnctst.audio3/play! :bgm/va3)"
                  :js "vnctst.audio3.js.play({bgm: \"va3\"})"
                  :desc "\"audio/bgm/va3.{ogg,mp3}\" をBGMとして再生する"
                  }
   :play-bgm-drop {:fn #(vnctst.audio3/play! :bgm/drop)
                   :cljs "(vnctst.audio3/play! :bgm/drop)"
                   :js "vnctst.audio3.js.play({bgm: \"drop\"})"
                   :desc "\"audio/bgm/drop.{ogg,mp3}\" をBGMとして再生する"
                   }
   :play-bgm-drop-2 {:fn #(vnctst.audio3/play! :bgm/drop 1.5 1.2 0.2)
                     :cljs "(vnctst.audio3/play! :bgm/drop 1.5 1.2 0.2)"
                     :js "vnctst.audio3.js.play({bgm: \"drop\"}, 1.5, 1.2, 0.2)"
                     :desc (str "\"audio/bgm/drop.{ogg,mp3}\" を"
                                "BGMとして再生する。"
                                "引数は音量(省略時1.0)、"
                                "ピッチ(再生速度倍率、省略時1.0)、"
                                "パン(左右に寄せる、省略時0、-1が左最大、"
                                "1が右最大)。"
                                "環境によってはピッチ、パンが無効な場合あり")
                     }
   :play-me-unmei {:fn #(vnctst.audio3/play! :me/unmei)
                   :cljs "(vnctst.audio3/play! :me/unmei)"
                   :js "vnctst.audio3.js.play({me: \"unmei\"})"
                   :desc "\"audio/me/unmei.{ogg,mp3}\" をMEとして再生する"
                   }
   :play-me-unmei-2 {:fn #(vnctst.audio3/play! :me/unmei 1.5 1.2 0.2)
                     :cljs "(vnctst.audio3/play! :me/unmei 1.5 1.2 0.2)"
                     :js "vnctst.audio3.js.play({me: \"unmei\"}, 1.5, 1.2, 0.2)"
                     :desc (str "\"audio/me/unmei.{ogg,mp3}\" を"
                                "MEとして再生する。"
                                "引数は音量(省略時1.0)、"
                                "ピッチ(再生速度倍率、省略時1.0)、"
                                "パン(左右に寄せる、省略時0、-1が左最大、"
                                "1が右最大)。"
                                "環境によってはピッチ、パンが無効な場合あり")
                     }
   :play-bgm-drop-ogg {:fn #(vnctst.audio3/bgm! "audio/bgm/drop.ogg")
                       :cljs "(vnctst.audio3/bgm! \"audio/bgm/drop.ogg\")"
                       :js "vnctst.audio3.js.bgm(\"audio/bgm/drop.ogg\")"
                       :desc (str "\"audio/bgm/drop.ogg\" を"
                                  "BGMとして再生する。"
                                  "任意のurlを指定可能"
                                  "(外部サーバ指定時は要CORS設定)。"
                                  "この環境でoggが再生可能かどうかは"
                                  "後述の方法で確認可能。"
                                  "再生できない環境の場合は何も再生されない。")
                       }
   :play-bgm-drop-mp3 {:fn #(vnctst.audio3/bgm! "audio/bgm/drop.mp3")
                       :cljs "(vnctst.audio3/bgm! \"audio/bgm/drop.mp3\")"
                       :js "vnctst.audio3.js.bgm(\"audio/bgm/drop.mp3\")"
                       :desc (str "\"audio/bgm/drop.mp3\" を"
                                  "BGMとして再生する。"
                                  "再生できない環境の場合は何も再生されない。")
                       }
   :play-me-unmei-ogg {:fn #(vnctst.audio3/me! "audio/me/unmei.ogg")
                       :cljs "(vnctst.audio3/me! \"audio/me/unmei.ogg\")"
                       :js "vnctst.audio3.js.me(\"audio/me/unmei.ogg\")"
                       :desc "\"audio/me/unmei.ogg\" をMEとして再生する"
                       }
   :play-bgm-nil {:fn #(vnctst.audio3/bgm! nil)
                  :cljs "(vnctst.audio3/bgm! nil)"
                  :js "vnctst.audio3.js.bgm(null)"
                  :desc "BGM / ME をフェード停止させる"
                  }
   :play-me-nil {:fn #(vnctst.audio3/me! nil)
                 :cljs "(vnctst.audio3/me! nil)"
                 :js "vnctst.audio3.js.me(null)"
                 :desc "BGM / ME をフェード停止させる"
                 }
   ;; BGS
   :stop-bgs {:fn #(vnctst.audio3/stop-bgs!)
              :cljs "(vnctst.audio3/stop-bgs!)"
              :js "vnctst.audio3.js.stopBgs()"
              :desc "BGSをフェード停止させる"
              }
   :stop-bgs-0 {:fn #(vnctst.audio3/stop-bgs! 0)
                :cljs "(vnctst.audio3/stop-bgs! 0)"
                :js "vnctst.audio3.js.stopBgs(0)"
                :desc "BGSを即座に停止させる(引数はフェード秒数)"
                }
   :play-bgs-noise {:fn #(vnctst.audio3/play! :bgs/noise)
                    :cljs "(vnctst.audio3/play! :bgs/noise)"
                    :js "vnctst.audio3.js.play({bgs: \"noise\"})"
                    :desc "\"audio/bgs/noise.{ogg,mp3}\" をBGSとして再生する"
                    }
   :play-bgs-nil {:fn #(vnctst.audio3/bgs! nil)
                  :cljs "(vnctst.audio3/bgs! nil)"
                  :js "vnctst.audio3.js.bgs(null)"
                  :desc "BGSをフェード停止させる"
                  }
   ;; SE
   :play-se-jump {:fn #(vnctst.audio3/play! :se/jump)
                  :cljs "(vnctst.audio3/play! :se/jump)"
                  :js "vnctst.audio3.js.play({se: \"jump\"})"
                  :desc (str "\"audio/se/jump.{ogg,mp3}\" をSEとして再生する。"
                             "連打での多重再生が可能")
                  }
   :play-se-yarare {:fn #(vnctst.audio3/play! :se/yarare)
                    :cljs "(vnctst.audio3/play! :se/yarare)"
                    :js "vnctst.audio3.js.play({se: \"yarare\"})"
                    :desc "\"audio/se/yarare.{ogg,mp3}\" をSEとして再生する"
                    }
   :play-se-yarare-ogg {:fn #(vnctst.audio3/se! "audio/se/yarare.ogg")
                        :cljs "(vnctst.audio3/se! \"audio/se/yarare.ogg\")"
                        :js "vnctst.audio3.js.se(\"audio/se/yarare.ogg\")"
                        :desc "\"audio/se/yarare.ogg\" をSEとして再生する"
                        }
   ;; Misc
   :set-volume-master-25 {:fn #(vnctst.audio3/set-volume-master! 0.25)
                          :cljs "(vnctst.audio3/set-volume-master! 0.25)"
                          :js "vnctst.audio3.js.setVolumeMaster(0.25)"
                          :desc (str "マスター音量を25%に設定する"
                                     "(音量値は0.0～1.0の範囲、初期値は0.5)")
                          }
   :set-volume-master-50 {:fn #(vnctst.audio3/set-volume-master! 0.5)
                          :cljs "(vnctst.audio3/set-volume-master! 0.5)"
                          :js "vnctst.audio3.js.setVolumeMaster(0.5)"
                          :desc "マスター音量を50%に設定する"
                          }
   :set-volume-master-100 {:fn #(vnctst.audio3/set-volume-master! 1.0)
                           :cljs "(vnctst.audio3/set-volume-master! 1.0)"
                           :js "vnctst.audio3.js.setVolumeMaster(1.0)"
                           :desc "マスター音量を100%に設定する"
                           }
   :can-play-ogg {:fn #(js/alert (vnctst.audio3/can-play-ogg?))
                  :cljs "(vnctst.audio3/can-play-ogg?)"
                  :js "vnctst.audio3.js.canPlayOgg()"
                  :desc "oggが再生可能なら真値を返す"
                  }
   :can-play-mp3 {:fn #(js/alert (vnctst.audio3/can-play-mp3?))
                  :cljs "(vnctst.audio3/can-play-mp3?)"
                  :js "vnctst.audio3.js.canPlayMp3()"
                  :desc "mp3が再生可能なら真値を返す"
                  }
   :can-play-m4a {:fn #(js/alert (vnctst.audio3/can-play-m4a?))
                  :cljs "(vnctst.audio3/can-play-m4a?)"
                  :js "vnctst.audio3.js.canPlayM4a()"
                  :desc "m4aが再生可能なら真値を返す"
                  }
   })



(defn- sync-button-labels! []
  (when-let [dom (js/document.getElementById "init-info")]
    (let [msg (if @display-js-mode?
                (str "vnctst.audio3.js.init({"
                     (string/join ", "
                                  (map (fn [[k v]]
                                         (str "\"" (name k) "\": " (pr-str v)))
                                       (seq (apply hash-map init-options))))
                     "})")
                (str "(vnctst.audio3/init! "
                     (string/join " " (map pr-str init-options))
                     ")")
                )]
      (set! (.. dom -textContent) msg)))
  (when-let [dom (js/document.getElementById "preload-info")]
    (let [msg (string/join (if @display-js-mode? ", " " ")
                           (map (if @display-js-mode?
                                  #(str "{"
                                        (namespace %)
                                        ":"
                                        (pr-str (name %))
                                        "}")
                                  pr-str)
                                (concat audio3/preset-bgm-keys
                                        audio3/preset-me-keys
                                        audio3/preset-bgs-keys
                                        audio3/preset-se-keys)))]
      (set! (.. dom -textContent) msg)))
  (doseq [[k m] (seq button-assign)]
    (when-let [dom (js/document.getElementById (name k))]
      ;(js/addEventListener dom "click" (:fn m))
      (aset dom "onclick" (:fn m))
      (set! (.. dom -textContent) (if @display-js-mode?
                                    (:js m)
                                    (:cljs m))))
    (when-let [dom (js/document.getElementById (str (name k) "-desc"))]
      (set! (.. dom -textContent) (:desc m)))))


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


(defn ^:export jsmode [bool]
  (reset! display-js-mode? bool)
  (sync-button-labels!))

(defn ^:export bootstrap []
  (sync-button-labels!)
  (apply audio3/init! init-options)
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



