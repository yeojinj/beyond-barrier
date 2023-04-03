# Use 'mlserver_env' python virtual environment!
# All the libraries are installed in the 'mlserver_env '

import os
import os.path
import face_recognition
import numpy as np
from PIL import Image, ImageDraw


# Select GPU Device
os.environ["CUDA_VISIBLE_DEVICES"] = '8'

# Declare lists
known_face_encodings = []
known_face_names = []

# Clear lists
known_face_encodings.clear()
known_face_names.clear()

# Folder lists
folders = os.listdir('./static/database')

for folder in folders:
    if (folder != ".ipynb_checkpoints"):
        route = './static/database/' + folder
        files = os.listdir(route)
        
        for file in files:
            if (not file.startswith('.')):
                # obama_1.jpg => obama_1, jpg
                name_include_number, ext = file.split('.')

                # obama_1 => obama, 1
                name, number = name_include_number.split('_')

                # load image file to recognize face
                image = face_recognition.load_image_file(route + "/" + file)

                # handle Exception
                if(len(face_recognition.face_encodings(image)) == 0): continue

                # encode the image
                globals()["{}_image_encoding_{}".format(name, number)] = face_recognition.face_encodings(image)[0]

                # append the encoded info into the list
                known_face_encodings.append(globals()["{}_image_encoding_{}".format(name, number)])

                # append the name into the list
                known_face_names.append(name)

                print("Learning faces...")
        
        # Check out the length of the lists
        print('Learned encoding for', route , len(known_face_encodings), 'images with', len(known_face_names), 'names.')


# Save the result as a file
np.save('server/known_face_encodings.npy', known_face_encodings)
np.save('server/known_face_names.npy', known_face_names)

        
