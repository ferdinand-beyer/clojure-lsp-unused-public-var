(ns lsp
  (:require [clj-commons.ansi :as ansi]
            [clojure-lsp.api :as clojure-lsp]
            [clojure.java.io :as io]))

(defn- clojure-lsp! [api-fn opts]
  (let [{:keys [result-code]} (api-fn opts)]
    (when-not (zero? result-code)
      (ansi/perr [:bold.red "Error:"] " Clojure-lsp reported issues, see output above.")
      (System/exit result-code))))

(def ^:private base-opts
  {#_#_:settings {:project-specs []}})

(defn- options [{:keys [project-root]}]
  (cond-> base-opts
    project-root (assoc :project-root (io/file project-root))))

(defn- fmt [opts]
  (clojure-lsp! clojure-lsp/clean-ns! opts)
  (clojure-lsp! clojure-lsp/format! opts))

(defn ^:export lint [params]
  (let [opts (options params)]
    ;; Ensure everything is analysed, and clj-kondo's cache is populated.
    ;; In particular, this should copy config of third-party libs and extract
    ;; inline configs from metadata to the `.clj-kondo/` directory.
    ;; Since we use clojure-lsp for development, the cache is probably already
    ;; hot locally, but might not be in CI, causing different results.
    (clojure-lsp/analyze-project-and-deps! opts)
    (clojure-lsp! clojure-lsp/diagnostics opts)
    (fmt (assoc opts :dry? true))))
