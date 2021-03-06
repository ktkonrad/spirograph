(ns spirograph.core
    (:require [reagent.core :as r]
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
(def width 500)
(def height 500)
(def frames-per-second 30)
(def rotations-per-second 0.5)

;;;; Drawing logic
;; ClojureScript doesn't support clojure Ratios, so do it manually.
;; TODO: use deftype and provide a ctor that does reduction.
(defrecord Ratio [numerator denominator value])
(defrecord Point [x y])
(defrecord Gear [radius-ratio offset-ratio angle])

(defn get-pos [gear]
  (let [center-x (/ width 2)
        center-y (/ height 2)
        R (/ width 2)
        r (* R (->> gear (:radius-ratio) (:value)))
        s (* r (->> gear (:offset-ratio) (:value)))
        th (:angle gear)
        ph (-> R (-) (/ r) (* th))]
    (Point.
     (-> R (- r) (* (q/cos th)) (+ (-> r (- s) (* (q/cos ph)))) (+ center-x))
     (-> R (- r) (* (q/sin th)) (+ (-> r (- s) (* (q/sin ph)))) (+ center-y)))))
     
(defn update-angle [angle]
  (+ angle (/ (* 2 Math/PI rotations-per-second) frames-per-second)))

(defn last-angle [angle]
  (- angle (/ (* 2 Math/PI rotations-per-second) frames-per-second)))
  
(defn get-period [gear]
  (* 2 Math/PI (->> gear (:radius-ratio) (:numerator))))

(defn draw-gear [gear]
  (if (< (:angle gear) (get-period gear))
    (let [old-pos (get-pos (update-in gear [:angle] last-angle))
          new-pos (get-pos gear)]
      (q/line (:x old-pos) (:y old-pos) (:x new-pos) (:y new-pos)))))

;;;; Quil drawing stuff
(defn setup[]
  (q/frame-rate frames-per-second)
  (q/background 255)
  ;; Returns initial state.
  {:angle 0
   :gear (Gear. (Ratio. 7 10 (/ 7 10)) (Ratio. 1 3 (/ 1 3)) 0)})

(defn update-state [state]
  (.log js/console (pr-str state))
  (update-in state [:gear :angle] update-angle state))

(defn draw [state]
  (draw-gear (:gear state)))

(def canvas-id "canvas")

(q/defsketch hello
  :host canvas-id
  :size [width height]
  :setup setup
  :update update-state
  :draw draw
  :middleware [m/fun-mode])

;; -------------------------
(defn ^:export sketch-start [id]
  (q/with-sketch (q/get-sketch-by-id id)
    (q/start-loop)))

(defn ^:export sketch-stop [id]
  (q/with-sketch (q/get-sketch-by-id id)
    (q/no-loop)))

;; Controls
(def running (r/atom true))

(defn stop-button []
  (let [start #(q/with-sketch (q/get-sketch-by-id canvas-id)
                 (q/start-loop))
        stop  #(q/with-sketch (q/get-sketch-by-id canvas-id)
                 (q/no-loop))]
    [:div
     [:input {:type "button"
              :value (if @running "stop" "start")
              :on-click (fn []
                          (if @running (stop) (start))
                          (swap! running not))}]]))

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to spirograph"]
   [:div [:canvas {:id "canvas"}]]
   [stop-button]
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
  (r/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
