import numpy as np
import tensorflow as tf
from PIL import Image
import os
import sys

def test_tflite(image_path, model_path, output_path):
    print(f"Loading TFLite model from {model_path}...")
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    input_shape = input_details[0]['shape']
    print(f"Model expects input shape: {input_shape}")
    
    # input_shape is usually [1, height, width, 3] for NHWC
    # In Sayan Nath's model, it's [1, 400, 600, 3] -> Height=400, Width=600
    target_height = input_shape[1]
    target_width = input_shape[2]

    print(f"Loading image from {image_path}...")
    if not os.path.exists(image_path):
        print(f"ERROR: Image not found at {image_path}")
        return

    # Load and resize
    img = Image.open(image_path).convert('RGB')
    print(f"Original image size: {img.size}")
    
    img_resized = img.resize((target_width, target_height), Image.Resampling.BILINEAR)
    print(f"Resized image to: {img_resized.size}")

    # Convert to numpy and normalize to [0, 1] as float32
    input_data = np.asarray(img_resized, dtype=np.float32) / 255.0
    input_data = np.expand_dims(input_data, axis=0) # Add batch dimension -> [1, 400, 600, 3]

    print("Running inference...")
    interpreter.set_tensor(input_details[0]['index'], input_data)
    interpreter.invoke()

    # Get output
    output_data = interpreter.get_tensor(output_details[0]['index'])
    print(f"Output shape: {output_data.shape}")

    # The output should be float32 in range [0, 1]. Convert to [0, 255] uint8
    output_image_np = (output_data[0] * 255.0).clip(0, 255).astype(np.uint8)
    
    # Save output
    output_image = Image.fromarray(output_image_np, mode='RGB')
    
    # Upscale back to original size just like the Android app
    output_image_full = output_image.resize(img.size, Image.Resampling.BILINEAR)
    
    output_image_full.save(output_path)
    print(f"Saved enhanced image to {output_path}")

if __name__ == "__main__":
    model_path = r"E:\University Programs\Stabila\feature\camera\src\main\assets\zero_dce.tflite"
    
    # You can change this path to any dark photo you have on your laptop
    input_image_path = r"E:\University Programs\Stabila\test_dark_photo.jpg"
    output_image_path = r"E:\University Programs\Stabila\test_enhanced_output.jpg"
    
    if not os.path.exists(input_image_path):
        print(f"Please place a dark photo named 'test_dark_photo.jpg' inside E:\\University Programs\\Stabila\\")
        sys.exit(1)
        
    test_tflite(input_image_path, model_path, output_image_path)
