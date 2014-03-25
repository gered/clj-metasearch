(ns clj-metasearch.core
  (:import (java.io File)
           (java.util.jar JarFile))
  (:require [clojure.tools.namespace.find :refer [find-clojure-sources-in-dir find-namespaces-in-jarfile]]
            [clojure.tools.namespace.file :refer [read-file-ns-decl]]
            [clojure.java.classpath :refer [classpath jar-file?]]))

(defn- find-namespaces-in-dirs [classpath-files]
  (->> classpath-files
       (filter (fn [^File file]
                 (.isDirectory file)))
       (map find-clojure-sources-in-dir)
       (apply concat)
       (map #(second (read-file-ns-decl %)))))

(defn- find-namespaces-in-jars [classpath-files]
  (->> classpath-files
       (filter jar-file?)
       (map #(new JarFile %))
       (map find-namespaces-in-jarfile)
       (apply concat)))

(defn- find-vars-in [namespace pred & [require-all-namespaces?]]
  (try
    (when require-all-namespaces?
      (require namespace))
    (->> (ns-interns namespace)
         (reduce
           (fn [matches [name var]]
             (let [metadata (meta var)]
               (if (pred metadata)
                 (conj matches {:ns namespace
                                :var var})
                 matches)))
           []))
    (catch Exception ex
      ; some namespaces, such as clojure.core.reducers, cannot be loaded under Java 6 and when
      ; we run this function on such a namespace we get an exception like:
      ;
      ;   java.lang.Exception: No namespace: clojure.core.reducers found
      ;
      ; which kind of makes it hard to pick out only those cases.
      ; also, the exact same type of exception will get thrown if we attempt to call
      ; (ns-interns) on a namespace which has not been loaded (required/used) yet.
      ;
      ; so for now we'll just silently fail on any exception and return a blank list (what
      ; else could we really do?)
      [])))

(defn find-namespaces
  "Searches for all Clojure namespaces currently on the classpath and returns only those
   for which pred returns true, or all Clojure namespaces if pred is not provided."
  [& [pred]]
  (let [classpath-files (classpath)
        dir-namespaces (find-namespaces-in-dirs classpath-files)
        jar-namespaces (find-namespaces-in-jars classpath-files)
        all-namespaces (distinct (concat dir-namespaces jar-namespaces))]
    (if pred
      (filter pred all-namespaces)
      all-namespaces)))

(defn find-vars
  "Finds vars in Clojure namespaces for which namespace-pred returns true that have metadata
   for which meta-pred returns true. If namespace-pred is not provided, all Clojure namespaces
   are checked. The returns vars will each be in a map where the :ns key is the namespace
   which the var was found in, and :var is the Clojure var itself (which you can get the value
   of by, e.g. using var-get)"
  [meta-pred & [namespace-pred]]
  (->> (find-namespaces namespace-pred)
       (map #(find-vars-in % meta-pred))
       (apply concat)))
