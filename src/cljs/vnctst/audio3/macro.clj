(ns vnctst.audio3.macro
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [project-clj.core :as project-clj]
            ))

;;; プリロードの為に、コンパイル時に特定ディレクトリ内のoggファイルを走査して
;;; その結果をコードとして埋め込む奴

(defn- get-filenames-from-dir-path [dir-path]
  (seq (.list (io/file dir-path))))

(defn- ogg-basenames [dir-path]
  (let [oggs (atom #{})
        mp3s (atom #{})
        m4as (atom #{})
        othre-files (atom #{})
        errors (atom nil)]
    (loop [files (get-filenames-from-dir-path dir-path)]
      (when (seq files)
        (let [filename (first files)
              ;; NB: extは今のところ、常に三文字固定
              [_ basename ext] (re-find #"^(.*)\.(...)$" filename)]
          (cond
            (= "ogg" ext) (swap! oggs conj basename)
            (= "mp3" ext) (swap! mp3s conj basename)
            (= "m4a" ext) (swap! m4as conj basename)
            :else (swap! othre-files conj filename))
          (recur (rest files)))))
    (when-not (empty? @othre-files)
      (swap! errors conj (apply print-str
                                "Found unknown files:" (seq @othre-files))))
    (let [diff-names-mp3 (clojure.set/difference @mp3s @oggs)
          diff-names-m4a (clojure.set/difference @m4as @oggs)
          diff-names-use-mp3? (< (count diff-names-mp3) (count diff-names-m4a))
          diff-target (if diff-names-use-mp3? "mp3" "m4a")
          diff-names (if diff-names-use-mp3? diff-names-mp3 diff-names-m4a)]
      (when-not (empty? diff-names)
        (swap! errors conj (apply print-str
                                  "Found to conflict between ogg and"
                                  diff-target ":" (seq diff-names))))
      [(vec @oggs) @errors])))

;;; dir-path に beep.ogg submit.ogg error.ogg が入っているとして、
;;; [:se/beep" :se/submit :se/error] のような式(もしくはnil)を返す。
(defmacro all-ogg-keys [mode dir-path]
  (assert (#{:bgm :bgs :me :se} mode) (str "invalid mode " (pr-str mode)))
  ;; マクロの制約上、今のところ、引数に文字列以外の式を渡す事はできない
  (assert (string? dir-path)
          (str "dir-path must be constant string (cannot be expr)"))
  (let [[basenames errors] (ogg-basenames dir-path)]
    (mapv #(keyword (name mode) %) basenames)))

;;; 上記の all-ogg-keys の為に、指定したディレクトリを検査する。
;;; 問題がなければnilを、問題があればエラー内容を文字列として返す。
;;; 検査内容は「oggファイルと同名のmp3/m3aファイルが揃っているか」
;;; 「変な拡張子のファイルがまぎれこんでいないか」のチェックなので、
;;; 「node-webkit等に組み込むので、再生環境はchrome固定に
;;; なるので、oggだけ入れていればよい」みたいな状況であれば、
;;; この検査はスキップしても問題ない。
(defmacro check-ogg-keys [mode dir-path]
  (assert (#{:bgm :bgs :me :se} mode) (str "invalid mode " (pr-str mode)))
  ;; マクロの制約上、今のところ、引数に文字列以外の式を渡す事はできない
  (assert (string? dir-path)
          (str "dir-path must be constant string (cannot be expr)"))
  (let [[basenames errors] (ogg-basenames dir-path)]
    (when-not (empty? errors)
      (apply string/join "\n" errors))))


