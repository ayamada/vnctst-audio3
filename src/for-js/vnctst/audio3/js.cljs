(ns vnctst.audio3.js
  (:require [vnctst.audio3 :as audio3]
            [project-clj.core :as project-clj]))


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



(def ^:export get_volume_master audio3/get-volume-master)
(def ^:export get_volume_bgm audio3/get-volume-bgm)
(def ^:export get_volume_bgs audio3/get-volume-bgs)
(def ^:export get_volume_me audio3/get-volume-me)
(def ^:export get_volume_se audio3/get-volume-se)

(def ^:export set_volume_master saudio3/et-volume-master!)
(def ^:export set_volume_bgm saudio3/et-volume-bgm!)
(def ^:export set_volume_bgs saudio3/et-volume-bgs!)
(def ^:export set_volume_me saudio3/et-volume-me!)
(def ^:export set_volume_se saudio3/et-volume-se!)



(defn ^:export stop_bgm [& [fade-sec]] (audio3/stop-bgm! fade-sec))
(defn ^:export stop_bgs [& [fade-sec]] (audio3/stop-bgs! fade-sec))
(defn ^:export stop_me [& [fade-sec]] (audio3/stop-me! fade-sec))
(defn ^:export stop_se [chan & [fade-sec]] (audio3/stop-se! chan fade-sec))



;;; jsではclojureのキーワードを簡単に指定できないので、
;;; (play! :se/hoge) のようなものを簡潔に表現できない。そこで、
;;; vnctst.audio3.js.play({se: "hoge"}) と指定できるようにする
(defn- conv-kp [key-or-path]
  (if-not (= js/Object (class key-or-path))
    key-or-path
    (let [m (js->clj (key-or-path))
          [k v] (first m)]
      ;(assert (= 1 (count m)))
      (assert (not (nil? k)) "k must be not null")
      (assert (not (nil? v)) "v must be not null")
      (keyword (str k) (str v)))))



(defn ^:export play_bgm [key-or-path & [vol pitch pan]]
  (audio3/play-bgm! (conv-kp key-or-path) vol pitch pan))

(defn ^:export play_bgs [key-or-path & [vol pitch pan]]
  (audio3/play-bgs! (conv-kp key-or-path) vol pitch pan))

(defn ^:export play_me [key-or-path & [vol pitch pan]]
  (audio3/play-me! (conv-kp key-or-path) vol pitch pan))

(defn ^:export play_se [key-or-path & [vol pitch pan]]
  (audio3/play-se! (conv-kp key-or-path) vol pitch pan))

(defn ^:export alarm [key-or-path & [vol pitch pan]]
  (audio3/alarm! (conv-kp key-or-path) vol pitch pan))

(defn ^:export play [k & [vol pitch pan]]
  (let [k (conv-kp k)]
    (assert (keyword? k)) ; 文字列指定不可
    (audio3/play! k vol pitch pan)))





(defn ^:export is_playing_bgm [] (audio3/playing-bgm?))
(defn ^:export is_playing_bgs [] (audio3/playing-bgs?))
(defn ^:export is_playing_me [] (audio3/playing-me?))


(defn ^:export preload_bgm [key-or-path]
  (audio3/preload-bgm! (conv-kp key-or-path)))
(def ^:export preload_bgs preload_bgm)
(def ^:export preload_me preload_bgm)

(defn ^:export unload_bgm [key-or-path]
  (audio3/unload-bgm! (conv-kp key-or-path)))
(def ^:export unload_bgs unload_bgm)
(def ^:export unload_me unload_bgm)

(defn ^:export is_preloaded_bgm [key-or-path]
  (audio3/preloaded-bgm? (conv-kp key-or-path)))
(defn ^:export is_succeeded_to_preload_bgm [key-or-path]
  (audio3/succeeded-to-preload-bgm? (conv-kp key-or-path)))




(defn ^:export is_preload_se [key-or-path]
  (audio3/preload-se! (conv-kp key-or-path)))
(defn ^:export is_unload_se [key-or-path]
  (audio3/unload-se! (conv-kp key-or-path)))
(defn ^:export is_loaded_se [key-or-path]
  (audio3/loaded-se? (conv-kp key-or-path)))
(defn ^:export is_succeeded_to_load_se [key-or-path]
  (audio3/succeeded-to-load-se? (conv-kp key-or-path)))







;; jsでsetを表現しづらいので、これは一旦非公開とする
;(def terminal-type util/terminal-type)

(defn ^:export can_play [mime] (audio3/can-play? mime))
(defn ^:export can_play_ogg [] (audio3/can-play-ogg?))
(defn ^:export can_play_mp3 [] (audio3/can-play-mp3?))
(defn ^:export can_play_m4a [] (audio3/can-play-m4a?))

;;; 0.0～1.0 の値と 0～100 のパーセント値を相互変換する。ボリューム値用。
(def ^:export float2percent audio3/float->percent)
(def ^:export percent2float audio3/percent->float)





;;; NB: jsではプリセット情報の取得ができないので、プリセット関係は提供しない



