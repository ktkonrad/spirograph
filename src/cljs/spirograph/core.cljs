(ns spirograph.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [quil.core :as q :include-macros true]
              [quil.middleware :as m])
    (:import goog.History))

;;--------------------------
;; Messing with quil

;;;; Constants
(def width 300)
(def height 300)
(def frames-per-second 30)
(def rotations-per-second 0.2)

;;;; Drawing logic
(defrecord Point [x y])
(defrecord Gear [radius offest])

(defn update-angle [angle]
  (+ angle (/ (* 2 Math/PI rotations-per-second) frames-per-second)))

(defn get-pos [gear angle]
  (let [center-x (/ width 2)
        center-y (/ height 2)
        R (/ width 2)
        r (:radius gear)
        s (:offset gear)
        th angle
        ph (-> R (-) (/ r) (* th))]
    (Point.
     (-> R (- r) (* (q/cos th)) (+ (-> r (- s) (* (q/cos ph)))) (+ center-x))
     (-> R (- r) (* (q/sin th)) (+ (-> r (- s) (* (q/sin ph)))) (+ center-y)))))
     
(defn draw-path [path]
  (let [endpoints (map list path (rest path))]
    (for [[p1 p2] endpoints]
      (q/line (:x p1) (:y p1) (:x p2) (:y p2)))))

;;;; Quil drawing stuff
(defn setup[]
  (q/frame-rate frames-per-second)
  ;; Returns initial state.
  {:angle 0
   :gear (Gear. (/ height 5) (/ height 9))})

(defn update-state [state]
  (.log js/console (pr-str state))
  (update-in state [:angle] update-angle))

(defn draw [state]
  (q/background 255)
  (q/fill 0)
  (let [ p (get-pos (:gear state) (:angle state))]
    (q/ellipse (:x p) (:y p) 5 5)))

(q/defsketch hello
  :host "canvas"
  :size [300 300]
  :setup setup
  :update update-state
  :draw draw
  :middleware [m/fun-mode])

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to spirograph"]
   [:div [:canvas {:id "canvas"}]]
   [:div [:a {:href "#/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About spirograph"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
