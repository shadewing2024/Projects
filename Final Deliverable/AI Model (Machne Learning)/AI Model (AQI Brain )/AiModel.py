
import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns

from tensorflow import keras
from tensorflow.keras import layers
from sklearn.preprocessing import StandardScaler

from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import train_test_split

# LOAD DATA
data = pd.read_excel('karachi_complete_data.xlsx')
print(data.head())

# CLEAN DATA
data = data.dropna()
data.columns = data.columns.str.replace('.', '_').str.lower()

# SELECT FEATURES
X = data[['temperature_2m', 'relative_humidity_2m', 'wind_speed_10m']]

y = data['main_aqi']

# HEATMAP
plt.figure(figsize=(6,4))
sns.heatmap(X.corr(), annot=True, fmt=".2f", cmap='viridis')
plt.title("Feature Correlation")
plt.show()

# SPLIT
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

# MODEL
scaler = StandardScaler()

X_train = scaler.fit_transform(X_train)
X_test = scaler.transform(X_test)

model = keras.Sequential([
    layers.Dense(64, activation='relu', input_shape=(X_train.shape[1],)),
    layers.Dense(32, activation='relu'),
    layers.Dense(1)
])

model.compile(
    optimizer='adam',
    loss='mean_squared_error',
    metrics=['mae']
)

model.fit(
    X_train, y_train,
    epochs=20,
    batch_size=32,
    validation_split=0.2
)

#predict
y_pred = model.predict(X_test)
y_pred = y_pred.flatten()

# EVALUATION
print("MAE:", mean_absolute_error(y_test, y_pred))
print("MSE:", mean_squared_error(y_test, y_pred))
print("R2 Score:", r2_score(y_test, y_pred))

# PLOT (LIMITED DATA)
plt.figure(figsize=(6,4))
plt.plot(range(len(y_test[:50])), y_test[:50], label='Actual', color='royalblue')
plt.plot(range(len(y_pred[:50])), y_pred[:50], label='Predicted', linestyle='--', color='lightblue')
plt.legend(loc='upper left')
plt.title("Actual vs Predicted AQI")
plt.show()

# SAVE MODEL
model.save("aqi_tf_model")
print("Model Saved!")

# Convert to tflite
import tensorflow as tf

converter = tf.lite.TFLiteConverter.from_saved_model("aqi_tf_model")
tflite_model = converter.convert()

with open("aqi_model.tflite", "wb") as f:
    f.write(tflite_model)

print("TFLite Model Created!")

