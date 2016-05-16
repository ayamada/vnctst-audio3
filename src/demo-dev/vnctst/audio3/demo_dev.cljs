(ns vnctst.audio3.demo-dev
  (:require-macros [project-clj.core :as project-clj])
  (:require [vnctst.audio3.demo]
            [figwheel.client :as fw]))

(enable-console-print!)

(let [hostname js/window.location.hostname
      port (project-clj/get-in [:figwheel :server-port] 3449)]
  (fw/start {:websocket-url (str "ws://" hostname ":" port "/figwheel-ws")
             ;:on-jsload on-jsload
             :heads-up-display false}))

