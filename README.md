<h1 align="center"> Real-Time Sign Language Recognition</h1>

<p align="center">
Hand gesture classification with computer vision, deep learning, and live webcam input.
</p>

---

## 📌 Overview

This project explores hand gesture recognition through image classification and live webcam detection.

It includes image preprocessing, feature visualization, training experiments with a custom CNN and VGG19, and a webcam demonstration that uses MediaPipe hand landmarks with a saved gesture recognition model.

The current implementation recognizes a predefined set of hand gestures. It does not translate continuous sign language or complete sentences.

---

## ✨ Features

- **Live webcam detection:** Capture frames and display predicted gesture labels.
- **Hand landmark visualization:** Draw detected hand landmarks and connections using MediaPipe.
- **Image preprocessing:** Resize images, convert them to grayscale, and normalize training inputs.
- **Feature exploration:** Calculate image statistics and visualize Local Binary Patterns.
- **Model training:** Train a custom CNN and a classifier using a frozen VGG19 backbone.
- **Training comparison:** Plot the final training accuracy of the CNN and VGG19 experiments.
- **Image collection:** Capture labeled webcam images for gesture experiments.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Python | Application and model development |
| TensorFlow / Keras | Neural network training and model loading |
| MediaPipe | Hand detection and landmark extraction |
| OpenCV | Webcam capture, image processing, and display |
| NumPy | Numerical operations and image arrays |
| scikit-learn | Training and test data splitting |
| Matplotlib | Image visualization and comparison plots |
| Pillow | Image utilities |
| Jupyter Notebook | Interactive development and experiments |

---

## ⚙️ How It Works

### Image Classification Experiments

1. Load gesture images from class folders in `Code/Dataset/`.
2. Resize images to **50 × 50 pixels** and convert them to grayscale.
3. Split the dataset into **80% training and 20% test data**.
4. Repeat the grayscale channel to produce three-channel model inputs.
5. Normalize pixel values and encode the class labels.
6. Train and compare two models:
   - **VGG19:** Frozen ImageNet backbone with a custom classification head, trained for 5 epochs.
   - **Custom CNN:** Three convolutional blocks followed by dense classification layers, trained for 10 epochs.

Both experiments use a batch size of 32 and reserve 10% of the training portion for validation.

### Live Webcam Recognition

1. Open the default webcam.
2. Detect one hand using MediaPipe.
3. Extract hand landmark coordinates.
4. Pass the coordinates to the saved `mp_hand_gesture` model.
5. Map the predicted class to a label from `gesture.names`.
6. Display the label and hand landmarks on the webcam feed.

The live demo uses a separate saved model; it does not load the CNN or VGG19 trained earlier in the notebook.

---

## 📂 Key Files

| Path | Description |
|---|---|
| `Code/MainFile.ipynb` | Preprocessing, model training, comparison plots, and live detection |
| `Code/Image Collection.ipynb` | Webcam image collection |
| `Code/Old code.ipynb` | Earlier implementation |
| `Code/Dataset/` | Gesture images used by the training notebook |
| `Code/ImageCollectionDataset/` | Image collection directory |
| `Code/mp_hand_gesture/` | Saved model loaded by the live demo |
| `Code/gesture.names` | Labels read by the live demo |
| `Code/label_map.json` | Class mapping for image classification artifacts |
| `Code/cnn_gesture_model.h5` | Included CNN model artifact |
| `Code/vgg19_gesture_model.h5` | Included VGG19 model artifact |
| `Code/gesture_model_final.h5` | Additional saved model artifact |
| `Code/X_train.npy`, `Code/X_test.npy` | Saved input arrays |
| `Code/y_train.npy`, `Code/y_test.npy` | Saved label arrays |

---

## 🚀 Getting Started

### Prerequisites

- Python and Jupyter Notebook
- A webcam for live detection
- A local desktop environment for OpenCV windows
- Compatible TensorFlow/Keras and MediaPipe versions

**Compatibility note:** The notebook uses legacy APIs, including `mp.solutions.hands` and directory-based model loading with `load_model('mp_hand_gesture')`. Newer package releases may require changes. The repository does not provide a pinned dependency environment.

### 1. Clone the Repository

```bash
git clone https://github.com/riteshvd/Real-Time-Sign-Language-Recogniton.git
cd Real-Time-Sign-Language-Recogniton
```

### 2. Create a Virtual Environment

```bash
python -m venv .venv
```

**Windows:**

```powershell
.venv\Scripts\Activate.ps1
```

**macOS / Linux:**

```bash
source .venv/bin/activate
```

### 3. Install Dependencies

The following lists the required packages. An unpinned installation may need version adjustments for the legacy APIs described above.

```bash
python -m pip install tensorflow mediapipe opencv-python numpy matplotlib scikit-learn pillow notebook
```

The image selection cell also uses Tkinter, which may require installation through your operating system.

### 4. Start Jupyter

```bash
cd Code
jupyter notebook
```

Open `MainFile.ipynb`.

Keep the notebook's working directory set to `Code` so its relative dataset, model, and label paths resolve correctly.

---

## 📷 Run the Webcam Demo

To try live detection without retraining:

1. Open `MainFile.ipynb`.
2. Locate the cell headed **LIVE GESTURE DETECTION WITH MODEL**.
3. Confirm that `mp_hand_gesture/` and `gesture.names` are available.
4. Run that cell.
5. Hold one hand in front of the webcam.
6. Press **q** while the OpenCV window is focused to stop.

The demo uses camera index `0`. Change `cv2.VideoCapture(0)` if your webcam uses a different index.

---

## 🧪 Train the Image Models

Run the preprocessing and training cells in order.

The notebook expects these dataset folders:

- `call_me`
- `fingers_crossed`
- `okay`
- `paper`
- `peace`
- `rock`
- `rock_on`
- `scissor`
- `thumbs`
- `up`

The VGG19 experiment may download ImageNet weights on its first run.

The comparison chart reports **training accuracy**, not held-out test accuracy. Evaluate the trained models on the test split before reporting generalization results.

---

## 🖼️ Collect Gesture Images

Open `Image Collection.ipynb`.

The default configuration captures **5 images per class** for:

- `thumbsup`
- `thumbsdown`
- `peace`
- `livelong`

Update `labels` and `number_imgs` to change the collection settings.

Collected images are saved under `ImageCollectionDataset/`. The training notebook reads from `Dataset/`, so collected images must be organized into the expected training folders before use.

---

## ⚠️ Current Limitations

- Recognition is limited to predefined gesture classes.
- Lighting, background, hand position, and camera quality can affect predictions.
- The live demo is configured for one hand.
- Training labels and webcam labels come from different mappings and should not be used interchangeably.
- The still-image prediction section uses nearest mean pixel intensity rather than CNN or VGG19 inference.
- The webcam code scales horizontal landmarks by frame height and vertical landmarks by frame width; this should be reviewed alongside the saved model's expected preprocessing.
- Webcam performance and model accuracy have not been independently benchmarked here.

---

## 🔮 Future Improvements

- Add a tested, pinned dependency file.
- Separate training and inference into Python scripts.
- Evaluate models using held-out accuracy, precision, recall, and confusion matrices.
- Standardize preprocessing and label mappings.
- Add camera-read error handling and prediction confidence thresholds.
- Expand the dataset across lighting conditions, backgrounds, and users.
- Explore temporal models for gestures involving movement.

---

## 👤 Contact

**Ritesh Varma Dommaraju**

[GitHub Profile](https://github.com/riteshvd)
