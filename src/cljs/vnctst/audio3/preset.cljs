(ns vnctst.audio3.preset
  (:require-macros [vnctst.audio3.macro :as m])
  (:require [vnctst.audio3.bgm :as bgm]
            [vnctst.audio3.se :as se]
            ))

;;; NB: 通常はseのみプリロードするとよい
;;;     (BGMは直前になってからプリロードした方が効率が良い)

(def all-bgm-keys (set (m/all-ogg-keys :bgm "resources/public/audio/bgm")))
(def all-bgm-error (m/check-ogg-keys :bgm "resources/public/audio/bgm"))
(def all-bgs-keys (set (m/all-ogg-keys :bgs "resources/public/audio/bgs")))
(def all-bgs-error (m/check-ogg-keys :bgs "resources/public/audio/bgs"))
(def all-me-keys (set (m/all-ogg-keys :me "resources/public/audio/me")))
(def all-me-error (m/check-ogg-keys :me "resources/public/audio/me"))
(def all-se-keys (set (m/all-ogg-keys :se "resources/public/audio/se")))
(def all-se-error (m/check-ogg-keys :se "resources/public/audio/se"))

(defn preload-all-bgm-preset! [& [silent?]]
  (when-let [e all-bgm-error]
    (when-not silent?
      (js/alert e)))
  (doseq [k all-bgm-keys]
    (bgm/preload! k))
  true)

(defn preload-all-bgs-preset! [& [silent?]]
  (when-let [e all-bgs-error]
    (when-not silent?
      (js/alert e)))
  (doseq [k all-bgs-keys]
    (bgm/preload! k))
  true)

(defn preload-all-me-preset! [& [silent?]]
  (when-let [e all-me-error]
    (when-not silent?
      (js/alert e)))
  (doseq [k all-me-keys]
    (bgm/preload! k))
  true)

(defn preload-all-se-preset! [& [silent?]]
  (when-let [e all-se-error]
    (when-not silent?
      (js/alert e)))
  (doseq [k all-se-keys]
    (se/preload! k))
  true)

