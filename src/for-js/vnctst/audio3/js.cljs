(ns vnctst.audio3.js
  (:require-macros [project-clj.core :as project-clj])
  (:require [vnctst.audio3 :as audio3]))


(def ^:export version (project-clj/get :version))



;;; javascript向けのインターフェースを提供する
;;; - js向けに、変数名内の一部の記号などを変更
;;; - 必要であれば、js向けに、引数/返り値の変換を行う
;;; - キーワード用のユーティリティを提供する？




(defn ^:export init [& [option]]
  (if-let [option (when option
                    (js->clj option :keywordize-keys true))]
    (audio3/init! option)
    (audio3/init!)))



(def ^:export getVolumeMaster audio3/get-volume-master)
(def ^:export getVolumeBgm audio3/get-volume-bgm)
(def ^:export getVolumeBgs audio3/get-volume-bgs)
(def ^:export getVolumeMe audio3/get-volume-me)
(def ^:export getVolumeSe audio3/get-volume-se)

(def ^:export setVolumeMaster audio3/set-volume-master!)
(def ^:export setVolumeBgm audio3/set-volume-bgm!)
(def ^:export setVolumeBgs audio3/set-volume-bgs!)
(def ^:export setVolumeMe audio3/set-volume-me!)
(def ^:export setVolumeSe audio3/set-volume-se!)



(defn ^:export stopBgm [& [fade-sec]] (audio3/stop-bgm! fade-sec))
(defn ^:export stopBgs [& [fade-sec]] (audio3/stop-bgs! fade-sec))
(defn ^:export stopMe [& [fade-sec]] (audio3/stop-me! fade-sec))
(defn ^:export stopSe [chan & [fade-sec]] (audio3/stop-se! chan fade-sec))



;;; jsではclojureのキーワードを簡単に指定できないので、
;;; (play! :se/hoge) のようなものを簡潔に表現できない。そこで、
;;; vnctst.audio3.js.play({se: "hoge"}) と指定できるようにする
(defn- conv-kp [key-or-path]
  (if-not (= js/Object (type key-or-path))
    key-or-path
    (let [m (js->clj key-or-path)
          [k v] (first m)]
      ;(assert (= 1 (count m)))
      (assert (not (nil? k)) "k must be not null")
      (assert (not (nil? v)) "v must be not null")
      (keyword (str k) (str v)))))



(defn ^:export playBgm [key-or-path & [vol pitch pan]]
  (audio3/play-bgm! (conv-kp key-or-path) vol pitch pan))

(defn ^:export playBgs [key-or-path & [vol pitch pan]]
  (audio3/play-bgs! (conv-kp key-or-path) vol pitch pan))

(defn ^:export playMe [key-or-path & [vol pitch pan]]
  (audio3/play-me! (conv-kp key-or-path) vol pitch pan))

(defn ^:export playSe [key-or-path & [vol pitch pan]]
  (audio3/play-se! (conv-kp key-or-path) vol pitch pan))

(defn ^:export alarm [key-or-path & [vol pitch pan]]
  (audio3/alarm! (conv-kp key-or-path) vol pitch pan))

(defn ^:export play [k & [vol pitch pan]]
  (let [k (conv-kp k)]
    (assert (keyword? k)) ; 文字列指定不可
    (audio3/play! k vol pitch pan)))





(defn ^:export isPlayingBgm [] (audio3/playing-bgm?))
(defn ^:export isPlayingBgs [] (audio3/playing-bgs?))
(defn ^:export isPlayingMe [] (audio3/playing-me?))


(defn ^:export preloadBgm [key-or-path]
  (audio3/preload-bgm! (conv-kp key-or-path)))
(def ^:export preloadBgs preload_bgm)
(def ^:export preloadMe preload_bgm)

(defn ^:export unloadBgm [key-or-path]
  (audio3/unload-bgm! (conv-kp key-or-path)))
(def ^:export unloadBgs unload_bgm)
(def ^:export unloadMe unload_bgm)

(defn ^:export isPreloadedBgm [key-or-path]
  (audio3/preloaded-bgm? (conv-kp key-or-path)))
(defn ^:export isSucceededToPreloadBgm [key-or-path]
  (audio3/succeeded-to-preload-bgm? (conv-kp key-or-path)))




(defn ^:export isPreloadSe [key-or-path]
  (audio3/preload-se! (conv-kp key-or-path)))
(defn ^:export isUnloadSe [key-or-path]
  (audio3/unload-se! (conv-kp key-or-path)))
(defn ^:export isLoadedSe [key-or-path]
  (audio3/loaded-se? (conv-kp key-or-path)))
(defn ^:export isSucceededToLoadSe [key-or-path]
  (audio3/succeeded-to-load-se? (conv-kp key-or-path)))







;; jsでsetを表現しづらいので、これは一旦非公開とする
;(def terminal-type util/terminal-type)

(defn ^:export canPlay [mime] (audio3/can-play? mime))
(defn ^:export canPlayOgg [] (audio3/can-play-ogg?))
(defn ^:export canPlayMp3 [] (audio3/can-play-mp3?))
(defn ^:export canPlayM4a [] (audio3/can-play-m4a?))

;;; 0.0～1.0 の値と 0～100 のパーセント値を相互変換する。ボリューム値用。
(def ^:export floatToPercent audio3/float->percent)
(def ^:export percentToFloat audio3/percent->float)





;;; NB: jsではプリセット情報の取得ができないので、プリセット関係は提供しない



