# clj-metasearch

Helper functions for searching through Clojure namespaces for Vars containing specific bits of metadata.

## Usage

Finding Clojure Vars is fairly simple.

```clojure
(use 'clj-metasearch.core)

(find-vars #(= (:name %) 'pprint))
=> ({:ns clojure.pprint, :var (var clojure.pprint/pprint)})
```

`find-vars` takes a predicate which will be run on the metadata for each Var being checked.

We can get the value of Vars we find by using `var-get` and then begin using the value right away. For example:

```clojure
(let [println-fn (-> (find-vars #(= (:name %) 'println))
                     (first)
                     :var
                     (var-get))]
  (println-fn "Hello world!"))
Hello world!
=> nil
```

By default `find-vars` will search all Clojure namespaces it can find in the current classpath. We can filter
which Clojure namespaces are checked by supplying an additional predicate to `find-vars` which will be run
on each namespace (as a symbol) found.

```clojure
; no namespace filtering. all namespaces are checked
(find-vars #(= (:name %) 'find-namespaces))
=> ({:ns clj-metasearch.core, :var (var clj-metasearch.core/find-namespaces)}
    {:ns clojure.tools.namespace.find, :var (var clojure.tools.namespace.find/find-namespaces)})

; using namespace filtering
(find-vars
  #(= (:name %) 'find-namespaces)
  #(not= % 'clj-metasearch.core))
=> ({:ns clojure.tools.namespace.find, :var (var clojure.tools.namespace.find/find-namespaces)})
```

By default, to help avoid loading a bunch of libraries the first time `find-vars` is called namespaces are not
automatically loaded before being checked. Thusly, you will only be able to find Vars in namespaces that are
currently loaded.

`find-vars` takes a third optional argument that allows you to change this behaviour. Passing `true` as the
third argument will cause each namespace being checked to first be loaded via `require`.

```clojure
(find-vars #(= (:name %) 'parse))
=> ()

(find-vars #(= (:name %) 'parse) nil true)
=> ({:ns clojure.xml, :var (var clojure.xml/parse)})
```

## License

Distributed under the the MIT License. See LICENSE for more details.
