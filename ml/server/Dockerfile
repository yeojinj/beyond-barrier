FROM python:3.9-slim

# Copy files
COPY . /app

# Work directory
WORKDIR /app

# Install packages
RUN pip install --upgrade pip
RUN apt-get update && apt-get install -y git
RUN python -m pip install git+https://github.com/openai/CLIP.git
RUN pip install flask
RUN pip install huggingface_hub
RUN pip install transformers
RUN pip install scikit-image
RUN pip install opencv-python
RUN apt-get -y install libgl1-mesa-glx
RUN apt-get -y install libglib2.0-0
RUN pip install pandas
RUN pip install psutil
RUN pip install matplotlib
RUN pip install seaborn


EXPOSE 5000
CMD ["python", "server.py"]
