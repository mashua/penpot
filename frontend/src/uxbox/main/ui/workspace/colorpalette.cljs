;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2015-2016 Andrey Antukh <niwi@niwi.nz>
;; Copyright (c) 2015-2016 Juan de la Cruz <delacruzgarciajuan@gmail.com>

(ns uxbox.main.ui.workspace.colorpalette
  (:require [beicon.core :as rx]
            [lentes.core :as l]
            [uxbox.store :as st]
            [uxbox.main.data.workspace :as dw]
            [uxbox.main.data.shapes :as uds]
            [uxbox.main.data.colors :as dc]
            [uxbox.main.ui.dashboard.colors :refer (collections-ref)]
            [uxbox.main.ui.workspace.base :as wb]
            [uxbox.main.ui.icons :as i]
            [uxbox.main.ui.keyboard :as kbd]
            [potok.core :as ptk]
            [uxbox.util.lens :as ul]
            [uxbox.util.data :refer (read-string)]
            [uxbox.util.color :refer (hex->rgb)]
            [uxbox.util.dom :as dom]
            [uxbox.util.mixins :as mx :include-macros true]))

(defn- get-selected-collection
  [local collections]
  (if-let [selected (:selected @local)]
    (first (filter #(= selected (:id %)) collections))
    (first collections)))

(mx/defc palette-items
  {:mixins [mx/static]}
  [colors position]
  (letfn [(select-color [event color]
            (dom/prevent-default event)
            (if (kbd/shift? event)
              (st/emit! (uds/update-selected-shapes-stroke {:color color}))
              (st/emit! (uds/update-selected-shapes-fill {:color color}))))]
    [:div.color-palette-content
     [:div.color-palette-inside {:style {:position "relative" :right (str (* position 52 10) "px")}}
      (for [hex-color colors
            :let [rgb-vec (hex->rgb hex-color)
                  rgb-color (apply str "" (interpose ", " rgb-vec))]]
        [:div.color-cell {:key (str hex-color)
                          :on-click #(select-color % hex-color)}
         [:span.color {:style {:background hex-color}}]
         [:span.color-text hex-color]
         [:span.color-text rgb-color]])]]))

(mx/defcs palette
  {:mixins [mx/static mx/reactive (mx/local)]}
  [own]
  (let [local (:rum/local own)
        collections (->> (mx/react collections-ref)
                         (vals)
                         (sort-by :name))
        collection (get-selected-collection local collections)]
    (letfn [(select-collection [event]
              (let [value (read-string (dom/event->value event))]
                (swap! local assoc :selected value :position 0)))
            (close [event]
              (st/emit! (dw/toggle-flag :colorpalette)))]
      [:div.color-palette
       [:div.color-palette-actions
        [:select.input-select {:on-change select-collection}
         (for [collection collections]
           [:option {:key (str (:id collection))
                     :value (pr-str (:id collection))}
            (:name collection "Storage")])]
        #_[:div.color-palette-buttons
           [:div.btn-palette.edit.current i/pencil]
           [:div.btn-palette.create i/close]]]
       [:span.left-arrow
        (if (> (:position @local) 0)
          {:on-click #(swap! local update :position dec)}
          {:class :disabled})
        i/arrow-slide]
       (palette-items (:colors collection) (:position @local))
       [:span.right-arrow
        (if (< (* (+ 1 (:position @local)) 10) (count (:colors collection)))
          {:on-click #(swap! local update :position inc)}
          {:class :disabled})
        i/arrow-slide]
       [:span.close-palette {:on-click close}
        i/close]])))

(defn- colorpalette-will-mount
  [own]
  (st/emit! (dc/fetch-collections))
  own)

(mx/defc colorpalette
  {:mixins [mx/static mx/reactive]
   :will-mount colorpalette-will-mount}
  []
  (let [flags (mx/react wb/flags-ref)]
    (when (contains? flags :colorpalette)
      (palette))))