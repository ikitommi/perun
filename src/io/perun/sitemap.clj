(set-env!
  :dependencies '[[org.clojure/clojure "1.6.0"]
                  [sitemap "0.2.4"]])

(ns io.perun.sitemap
  {:boot/export-tasks true}
  (:require [boot.core       :as boot]
            [boot.util       :as u]
            [io.perun.core   :as perun]
            [clojure.java.io :as io]
            [sitemap.core    :as sitemap-gen]))


(def ^:private
  +defaults+ {:filename "sitemap.xml"
              :target "public"
              :datafile "meta.edn"})

(defn create-sitemap [files options]
  (map
    (fn [file]
      {:loc (str (:url options) (:filename file))
       :lastmod (:date_modified file)
       :changefreq (or (:sitemap_changefreq file) "weekly")
       :priority (or (:sitemap_priority file) 0.8)})
    files))

(boot/deftask sitemap
  "Generate sitemap"
  [f filename FILENAME str "Generated sitemap filename"
   o target   OUTDIR   str "The output directory"
   d datafile DATAFILE str "Datafile with all parsed meta information"
   u url      URL      str "Base URL"]
  (let [tmp (boot/temp-dir!)]
    (fn middleware [next-handler]
      (fn handler [fileset]
        (let [options (merge +defaults+ *opts*)
              files (perun/read-files-defs fileset (:datafile options))
              sitemap-filepath (str (:target options) "/" (:filename options))
              sitemap-xml (create-sitemap files options)
              sitemap-string (sitemap-gen/generate-sitemap sitemap-xml)]
          (perun/create-file tmp sitemap-filepath sitemap-string)
          (u/info (str "Generate sitemap and save to " sitemap-filepath "\n"))
          (perun/commit-and-next fileset tmp next-handler))))))

