(ns vnctst.audio3.preset
  (:require-macros [vnctst.audio3.macro :as m])
  (:require [vnctst.audio3.bgm :as bgm]
            [vnctst.audio3.se :as se]
            ))

;;; NB: 通常はseのみプリロードするとよい
;;;     (BGMは直前になってからプリロードした方が効率が良い)

(def all-bgm-keys (m/all-ogg-keys :bgm "resources/public/audio/bgm"))
(def all-bgm-errors (m/check-ogg-keys :bgm "resources/public/audio/bgm"))
(def all-bgs-keys (m/all-ogg-keys :bgs "resources/public/audio/bgs"))
(def all-bgs-errors (m/check-ogg-keys :bgs "resources/public/audio/bgs"))
(def all-me-keys (m/all-ogg-keys :me "resources/public/audio/me"))
(def all-me-errors (m/check-ogg-keys :me "resources/public/audio/me"))
(def all-se-keys (m/all-ogg-keys :se "resources/public/audio/se"))
(def all-se-errors (m/check-ogg-keys :se "resources/public/audio/se"))

(defn preload-all-bgm-preset! [& [silent?]]
  (when-let [e all-bgm-errors]
    (when-not silent?
      (js/alert e)))
  (doseq [k all-bgm-keys]
    (bgm/preload! k)))

(defn preload-all-bgs-preset! [& [silent?]]
  (when-let [e all-bgs-errors]
    (when-not silent?
      (js/alert e)))
  (doseq [k all-bgs-keys]
    (bgm/preload! k)))

(defn preload-all-me-preset! [& [silent?]]
  (when-let [e all-me-errors]
    (when-not silent?
      (js/alert e)))
  (doseq [k all-me-keys]
    (bgm/preload! k)))

(defn preload-all-se-preset! [& [silent?]]
  (when-let [e all-se-errors]
    (when-not silent?
      (js/alert e)))
  (doseq [k all-se-keys]
    (se/preload! k)))

