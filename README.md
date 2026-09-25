# Real-Time Sign Language Recognition

A computer vision project for classifying hand gestures from images and detecting gestures through a live webcam feed. Built with Python, TensorFlow/Keras, OpenCV, and MediaPipe.

## Overview

This project explores two approaches to hand gesture recognition:

- **Image classification:** Training a custom CNN and a VGG19-based classifier on labeled gesture images.
- **Live recognition:** Extracting hand landmarks with MediaPipe and predicting gestures using a saved model.

The project includes image collection, preprocessing, training, visualization, and webcam inference. It focuses on a fixed set of gestures rather than continuous sign language translation.

## Tech Stack

| Area | Tools |
|---|---|
| Programming | Python |
| Deep Learning | TensorFlow, Keras |
| Computer Vision | OpenCV, MediaPipe |
| Data Processing | NumPy, scikit-learn |
| Visualization | Matplotlib |
| Development | Jupyter Notebook |

## Features

- Webcam gesture prediction with labels displayed on the video feed.
- Hand landmark detection and visualization.
- Image resizing, grayscale conversion, and normalization.
- Image statistics and Local Binary Pattern visualization.
- Custom CNN and VGG19 training experiments.
- Training accuracy comparison.
- Labeled image collection using a webcam.

## Implementation

### Image Classification

Images are loaded from class folders in `Code/Dataset/`, resized to **50 × 50 pixels**, and converted to grayscale. The grayscale channel is repeated to create three-channel inputs, and pixel values are normalized.

The dataset is split into **80% training and 20% testing**, with 10% of the training portion reserved for validation.

| Model | Architecture | Training |
|---|---|---|
| Custom CNN | Three convolutional blocks, pooling, dropout, and dense layers | 10 epochs |
| VGG19 | Frozen ImageNet backbone with a custom classification head | 5 epochs |

Both models use a batch size of 32 and classify 10 gesture categories.

### Live Recognition

The webcam workflow uses a separate saved model:

1. Capture a frame using OpenCV.
2. Detect a hand and extract landmarks using MediaPipe.
3. Pass the landmark coordinates to `mp_hand_gesture`.
4. Read the predicted label from `gesture.names`.
5. Display the label and hand landmarks.

The webcam demo does not use the CNN or VGG19 models trained in the earlier notebook cells.

## Project Files

| Path | Purpose |
|---|---|
| `Code/MainFile.ipynb` | Preprocessing, training, comparison, and webcam recognition |
| `Code/Image Collection.ipynb` | Capture labeled gesture images |
| `Code/Old code.ipynb` | Earlier implementation |
| `Code/Dataset/` | Image classification dataset |
| `Code/ImageCollectionDataset/` | Collected webcam images |
| `Code/mp_hand_gesture/` | Saved model used for live recognition |
| `Code/gesture.names` | Labels for webcam predictions |
| `Code/label_map.json` | Image classification label mapping |
| `Code/*.h5` | Saved model artifacts |
| `Code/*.npy` | Saved training and test arrays |

## Setup

### Requirements

- Python
- Jupyter Notebook
- A webcam for live recognition
- A local desktop environment for OpenCV windows

The notebooks use legacy TensorFlow/Keras and MediaPipe APIs. Dependency versions are not pinned, so compatibility adjustments may be needed.

### Clone the Repository

```bash
git clone https://github.com/riteshvd/Real-Time-Sign-Language-Recogniton.git
cd Real-Time-Sign-Language-Recogniton
```

### Create a Virtual Environment

```bash
python -m venv .venv
```

Activate on Windows:

```powershell
.venv\Scripts\Activate.ps1
```

Activate on macOS or Linux:

```bash
source .venv/bin/activate
```

### Install Packages

```bash
python -m pip install tensorflow mediapipe opencv-python numpy matplotlib scikit-learn pillow notebook
```

The image selection cell also requires Tkinter.

**Compatibility:** The live demo uses `mp.solutions.hands` and loads a SavedModel directory through `load_model()`. Use package versions that support these APIs or update the loading and detection code.

### Open the Notebook

```bash
cd Code
jupyter notebook
```

Open `MainFile.ipynb`. Keep the working directory set to `Code` so the relative paths resolve correctly.

## Usage

### Run Live Recognition

1. Open `MainFile.ipynb`.
2. Find the cell labeled `LIVE GESTURE DETECTION WITH MODEL`.
3. Confirm that `mp_hand_gesture/` and `gesture.names` are present.
4. Run the cell and hold one hand in front of the webcam.
5. Press **q** in the video window to exit.

The demo uses `cv2.VideoCapture(0)`. Change the camera index if needed. Running the training cells is not required for this demo.

### Train the Models

Run the preprocessing and training cells in order. The notebook expects these folders inside `Dataset/`:

```text
call_me
fingers_crossed
okay
paper
peace
rock
rock_on
scissor
thumbs
up
```

VGG19 may download ImageNet weights during the first run.

### Collect Images

Open `Image Collection.ipynb` and update:

- `labels` to select gesture categories.
- `number_imgs` to set the number of images per category.

The default collects five images each for `thumbsup`, `thumbsdown`, `peace`, and `livelong`.

Images are saved to `ImageCollectionDataset/`. Organize them into the appropriate `Dataset/` folders before using them for training.

## Evaluation and Limitations

- The comparison chart shows final **training accuracy**, not held-out test accuracy.
- The still-image prediction section uses mean pixel intensity matching rather than neural network inference.
- The training workflow and webcam demo use different label mappings.
- Live detection supports one hand and depends on lighting, camera quality, and hand position.
- The webcam landmark scaling uses swapped frame dimensions and needs review against the saved model's preprocessing.
- The project does not perform sentence-level sign language translation.

## Planned Improvements

- Add pinned dependencies and reproducible setup instructions.
- Separate training and inference into Python scripts.
- Evaluate both image models on the test split.
- Add confusion matrices, precision, recall, and F1 scores.
- Standardize label mappings and preprocessing.
- Improve camera error handling and add confidence thresholds.
- Expand the dataset and explore recognition of gestures involving movement.

## Contact

**Ritesh Varma Dommaraju**  
[GitHub](https://github.com/riteshvd)
