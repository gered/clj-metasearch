(ns clj-metasearch.core
  (:import (java.io File)
           (java.util.jar JarFile)
           (clojure.lang Compiler$CompilerException))
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

(defn- namespace-not-found-exception? [^Exception ex]
  (not (nil? (re-matches #"^(?:java\.lang\.Exception\: No namespace\: )(.*?)(?: found)$" (.toString ex)))))

(defn- find-vars-in [namespace pred {:keys [require-all-namespaces? throw-exceptions? throw-compiler-exceptions?]}]
  (try
    (when require-all-namespaces?
      (require namespace))
    (->> (ns-interns namespace)
         (reduce
           (fn [matches [name var]]
             (let [metadata (meta var)]
               (if (pred metadata)
                 (conj matches var)
                 matches)))
           []))
    (catch Compiler$CompilerException ex
      (if (or throw-exceptions? throw-compiler-exceptions?)
        (throw ex)
        []))
    (catch Exception ex
      (if throw-exceptions?
        (throw ex)
        []))))

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
   are checked.

   The final 'options' argument accepts the following:

   :require-all-namespaces?
   Determines if namespaces will be 'required' (loaded) before they are scanned for vars. If
   you specify false for this option then only vars from namespaces which are already loaded
   will be searched. This option in combination with namespace-pred can be used to
   significantly reduce the number of namespaces that are loaded and scanned through. The
   default for this option is false.

   :throw-exceptions?
   If true, any exceptions encountered while searching through namespaces will not be
   suppressed, meaning it will be the callers responsibility to deal with them. The default
   for this option is false, meaning all exceptions are silently suppressed.

   :throw-compiler-exceptions?
   Similar to :throw-exceptions?, but only clojure.lang.Compiler.CompilerException exceptions
   will be left unsuppressed. All other exceptions will be silently suppressed. The default
   for this option is false.

   A sequence of maps will be returned, where each map holds information about a var that was
   found. The :ns key is the namespace which the var was found in, and :var is the Clojure var
   itself (which you can get the value of by, e.g. using var-get)"
  [meta-pred & {:keys [namespace-pred] :as options}]
  (->> (find-namespaces namespace-pred)
       (map #(find-vars-in % meta-pred options))
       (apply concat)))
