(ns refactor-nrepl.plugin
  (:require
   [refactor-nrepl.core :as core]
   [leiningen.core.main :as lein]))

(def ^:private min-clojure-version
  "1.7.0")

(defn- find-clojure-version [dependencies]
  (->> dependencies
       (some (fn [[id version & _]]
               (when (= id 'org.clojure/clojure)
                 version)))))

(defn middleware
  [{:keys [dependencies exclusions] :as project}]
  (let [clojure-excluded? (some #(= % 'org.clojure/clojure) exclusions)
        clojure-version (when-not clojure-excluded?
                          (find-clojure-version dependencies))
        clojure-version-ok?
        (cond clojure-excluded? true ; up to the user

              (nil? clojure-version)
              ;; Lein 2.5.2+ uses Clojure 1.7 by default
              (lein/version-satisfies? (lein/leiningen-version) "2.5.2")

              :else
              (lein/version-satisfies? clojure-version min-clojure-version))]
    (if clojure-version-ok?
      (-> project
          (update-in [:dependencies]
                     (fnil into [])
                     [['refactor-nrepl (core/version)]])
          (update-in [:repl-options :nrepl-middleware]
                     (fnil into [])
                     '[refactor-nrepl.middleware/wrap-refactor]))
      (do
        (lein/warn "Warning: refactor-nrepl requires Clojure version"
                   min-clojure-version "or later.")
        (lein/warn "Warning: refactor-nrepl middleware won't be activated." )
        project))))
