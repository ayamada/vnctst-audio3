(def use-advanced-optimizations? true)

(def compiler-option-common
  {:main 'vnctst.audio3
   :output-to "resources/public/cljs/cl.js"
   :language-in :ecmascript5
   :language-out :ecmascript5
   :optimizations (if use-advanced-optimizations? :advanced :simple)
   :pretty-print false})

(def compiler-option-prod compiler-option-common)

(def compiler-option-js
  (merge compiler-option-common
         {:output-to "vnctst-audio3.js"}))

(def compiler-option-dev
  (merge compiler-option-common
         {:output-dir "resources/public/cljs/out"
          :asset-path "cljs/out"
          :optimizations :none
          :source-map true
          :source-map-timestamp true
          :cache-analysis true
          :pretty-print true
          }))



(defproject jp.ne.tir/vnctst-audio3 "0.1.0-SNAPSHOT"
  :description "audio playback library for html5 game"
  :url "https://github.com/ayamada/vnctst-audio3"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/core.async "0.2.374"]]
  :plugins [[lein-cljsbuild "1.1.3"]]
  :source-paths ["src/cljs"]
  :clean-targets ^{:protect false} [:target-path
                                    :compile-path
                                    "resources/public/cljs"
                                    "figwheel_server.log"]
  ;; NB: リリース時には resources/ 配下は明示的に含めないようにする
  ;;     (開発時に使うものを入れており、リリース時には全く不要な為)
  :resource-paths []
  :jar-exclusions []
  ;; 基本的に、以下の場合分けになる
  ;; - 開発用(サンプルデモ使用)
  ;;   - ringサーバ起動
  ;;     - lein with-profile ring trampoline ring server-headless
  ;;   - figwheelサーバ起動
  ;;     - rlwrap lein with-profile cljs,js,demo-dev figwheel demo-dev
  ;; - リリース用
  ;;   - clojars登録用(profileなし状態)
  ;;     - lein deploy clojars
  ;;   - vnctst-audio3.js のリリース版ビルド(ビルド後にコミットする想定)
  ;;     - lein with-profile cljs,js cljsbuild once js
  ;;   - サンプルデモのリリースビルド
  ;;     - lein with-profile cljs,js,demo-prod cljsbuild once demo-prod
  :profiles {:cljs {:dependencies [[org.clojure/clojurescript "1.8.51"]]}
             :ring {:source-paths ["src/ring"]
                    :dependencies[[ring/ring-core "1.4.0"]
                                  [hiccup "1.0.5"]]
                    :resource-paths ["resources"]
                    :plugins [[lein-ring "0.9.7"]]
                    :ring {:port 8018
                           :handler vnctst.audio3.demo-server/handler}}
             :js {:dependencies [[jp.ne.tir/project-clj "0.1.6"]]
                  :source-paths ["src/cljs" "src/for-js"]}
             :demo-dev {:source-paths ["src/cljs" "src/for-js"
                                       "src/demo" "src/demo-dev"]
                        :resource-paths ["resources"]
                        :dependencies [[figwheel "0.5.3"]]
                        :plugins [[lein-figwheel "0.5.3"]]}
             :demo-prod {:source-paths ["src/cljs" "src/for-js" "src/demo"]
                         :resource-paths ["resources"]}}
  :cljsbuild {:builds {:js {:compiler ~compiler-option-js
                            :jar true}
                       :demo-dev {:compiler ~compiler-option-dev
                                  :jar false}
                       :demo-prod {:compiler ~compiler-option-prod
                                   :jar true}
                       }}
  :figwheel {:http-server-root "public"
             :server-port 9018
             :server-logfile "figwheel_server.log"}
  )
