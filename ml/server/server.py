from flask import *
import os


######################## SELECT #######################
# Select GPU Device
os.environ["CUDA_VISIBLE_DEVICES"] = '5'
#######################################################


from huggingface_hub import hf_hub_download
coco_weight = hf_hub_download(repo_id="akhaliq/CLIP-prefix-captioning-COCO-weights", filename="coco_weights.pt")
import clip
from torch import nn
import numpy as np
import torch
import torch.nn.functional as nnf
import sys
from typing import Tuple, List, Union, Optional
from transformers import GPT2Tokenizer, GPT2LMHeadModel, AdamW, get_linear_schedule_with_warmup
from tqdm import tqdm, trange
import skimage.io as io
import PIL.Image


##### logo_detection start

######################## SELECT #######################

##### (1) GPU Server 모델 설정
logo_model = torch.hub.load('ultralytics/yolov5', 'custom', path='server/best.pt', _verbose=False)

##### (2) Local Server 모델 설정
# logo_model = torch.hub.load('ultralytics/yolov5', 'custom', path='./best.pt', _verbose=False)

#######################################################

logo_model.cuda()

def logodetect(img):
    
    ######################## SELECT #######################
    
    ##### (1) S3 link를 이용하여 load
    image = io.imread(img)
    im = PIL.Image.fromarray(image)  # PIL image
    
    ##### (2) Image File을 load
    # im = PIL.Image.open(img)  # PIL image
    
    #######################################################
    
    results = logo_model(im)
    df = results.pandas().xyxy[0]  # result in pandas.DataFrame

    for i in range(0, df.shape[0]):
        row = df.iloc[i]
        if row['confidence'] >= 0.9:
            return int(row['name'])
        
    # if logo detection fails
    return -1

##### logo_detection end


N = type(None)
V = np.array
ARRAY = np.ndarray
ARRAYS = Union[Tuple[ARRAY, ...], List[ARRAY]]
VS = Union[Tuple[V, ...], List[V]]
VN = Union[V, N]
VNS = Union[VS, N]
T = torch.Tensor
TS = Union[Tuple[T, ...], List[T]]
TN = Optional[T]
TNS = Union[Tuple[TN, ...], List[TN]]
TSN = Optional[TS]
TA = Union[T, ARRAY]
GPT2 = GPT2LMHeadModel.from_pretrained('gpt2')
D = torch.device
CPU = torch.device('cpu')


def get_device(device_id: int) -> D:
    if not torch.cuda.is_available():
        return CPU
    device_id = min(torch.cuda.device_count() - 1, device_id)
    return torch.device(f'cuda:{device_id}')

CUDA = get_device


class MLP(nn.Module):

    def forward(self, x: T) -> T:
        return self.model(x)

    def __init__(self, sizes: Tuple[int, ...], bias=True, act=nn.Tanh):
        super(MLP, self).__init__()
        layers = []
        for i in range(len(sizes) -1):
            layers.append(nn.Linear(sizes[i], sizes[i + 1], bias=bias))
            if i < len(sizes) - 2:
                layers.append(act())
        self.model = nn.Sequential(*layers)

############################
# 시간 측정을 위한 임시 코드
# import time

# def logging_time(original_fn):
#     def wrapper_fn(*args, **kwargs):
#         start_time = time.time()
#         result = original_fn(*args, **kwargs)
#         end_time = time.time()
#         print("WorkingTime[{}]: {} sec".format(original_fn.__name__, end_time - start_time))
#         return result
#     return wrapper_fn

############################
class ClipCaptionModel(nn.Module):

    # @logging_time
    #@functools.lru_cache #FIXME
    def get_dummy_token(self, batch_size: int, device: D) -> T:
        return torch.zeros(batch_size, self.prefix_length, dtype=torch.int64, device=device)

    # @logging_time
    def forward(self, tokens: T, prefix: T, mask: Optional[T] = None, labels: Optional[T] = None):
        embedding_text = self.gpt.transformer.wte(tokens)
        prefix_projections = self.clip_project(prefix).view(-1, self.prefix_length, self.gpt_embedding_size)
        #print(embedding_text.size()) #torch.Size([5, 67, 768])
        #print(prefix_projections.size()) #torch.Size([5, 1, 768])
        embedding_cat = torch.cat((prefix_projections, embedding_text), dim=1)
        if labels is not None:
            dummy_token = self.get_dummy_token(tokens.shape[0], tokens.device)
            labels = torch.cat((dummy_token, tokens), dim=1)
        out = self.gpt(inputs_embeds=embedding_cat, labels=labels, attention_mask=mask)
        return out

    # @logging_time
    def __init__(self, prefix_length: int, prefix_size: int = 512):
        
        # (1) 불러오기
        # start = time.time()
        super(ClipCaptionModel, self).__init__()
        # end = time.time()
        # print("(1): " + f"{end - start:.5f} sec")
        
        # (2) GP2LMHeadModel
        # start = time.time()
        self.prefix_length = prefix_length
        # end = time.time()
        # print("(2-1): " + f"{end - start:.5f} sec")
        
        # start = time.time()
        self.gpt = GPT2
        # end = time.time()
        # print("(2-2): " + f"{end - start:.5f} sec")
        
        # start = time.time()
        self.gpt_embedding_size = self.gpt.transformer.wte.weight.shape[1]
        # end = time.time()
        # print("(2-3): " + f"{end - start:.5f} sec")
        
        
        # (3) 
        # start = time.time()
        if prefix_length > 10:  # not enough memory
            # print("(3-1)")
            self.clip_project = nn.Linear(prefix_size, self.gpt_embedding_size * prefix_length)
        else:
            # print("(3-2)")
            self.clip_project = MLP((prefix_size, (self.gpt_embedding_size * prefix_length) // 2, self.gpt_embedding_size * prefix_length))

        # end = time.time()
        # print("(2): " + f"{end - start:.5f} sec")

class ClipCaptionPrefix(ClipCaptionModel):

    def parameters(self, recurse: bool = True):
        return self.clip_project.parameters()

    def train(self, mode: bool = True):
        super(ClipCaptionPrefix, self).train(mode)
        self.gpt.eval()
        return self
        

#@title Caption prediction
def generate_beam(model, tokenizer, beam_size: int = 5, prompt=None, embed=None,
                  entry_length=67, temperature=1., stop_token: str = '.'):

    model.eval()
    stop_token_index = tokenizer.encode(stop_token)[0]
    tokens = None
    scores = None
    device = next(model.parameters()).device
    seq_lengths = torch.ones(beam_size, device=device)
    is_stopped = torch.zeros(beam_size, device=device, dtype=torch.bool)
    with torch.no_grad():
        if embed is not None:
            generated = embed
        else:
            if tokens is None:
                tokens = torch.tensor(tokenizer.encode(prompt))
                tokens = tokens.unsqueeze(0).to(device)
                generated = model.gpt.transformer.wte(tokens)
        for i in range(entry_length):
            outputs = model.gpt(inputs_embeds=generated)
            logits = outputs.logits
            logits = logits[:, -1, :] / (temperature if temperature > 0 else 1.0)
            logits = logits.softmax(-1).log()
            if scores is None:
                scores, next_tokens = logits.topk(beam_size, -1)
                generated = generated.expand(beam_size, *generated.shape[1:])
                next_tokens, scores = next_tokens.permute(1, 0), scores.squeeze(0)
                if tokens is None:
                    tokens = next_tokens
                else:
                    tokens = tokens.expand(beam_size, *tokens.shape[1:])
                    tokens = torch.cat((tokens, next_tokens), dim=1)
            else:
                logits[is_stopped] = -float(np.inf)
                logits[is_stopped, 0] = 0
                scores_sum = scores[:, None] + logits
                seq_lengths[~is_stopped] += 1
                scores_sum_average = scores_sum / seq_lengths[:, None]
                scores_sum_average, next_tokens = scores_sum_average.view(-1).topk(beam_size, -1)
                next_tokens_source = next_tokens // scores_sum.shape[1]
                seq_lengths = seq_lengths[next_tokens_source]
                next_tokens = next_tokens % scores_sum.shape[1]
                next_tokens = next_tokens.unsqueeze(1)
                tokens = tokens[next_tokens_source]
                tokens = torch.cat((tokens, next_tokens), dim=1)
                generated = generated[next_tokens_source]
                scores = scores_sum_average * seq_lengths
                is_stopped = is_stopped[next_tokens_source]
            next_token_embed = model.gpt.transformer.wte(next_tokens.squeeze()).view(generated.shape[0], 1, -1)
            generated = torch.cat((generated, next_token_embed), dim=1)
            is_stopped = is_stopped + next_tokens.eq(stop_token_index).squeeze()
            if is_stopped.all():
                break
    scores = scores / seq_lengths
    output_list = tokens.cpu().numpy()
    output_texts = [tokenizer.decode(output[:int(length)]) for output, length in zip(output_list, seq_lengths)]
    order = scores.argsort(descending=True)
    output_texts = [output_texts[i] for i in order]
    return output_texts


def generate2(
        model,
        tokenizer,
        tokens=None,
        prompt=None,
        embed=None,
        entry_count=1,
        entry_length=67,  # maximum number of words
        top_p=0.8,
        temperature=1.,
        stop_token: str = '.',
):
    model.eval()
    generated_num = 0
    generated_list = []
    stop_token_index = tokenizer.encode(stop_token)[0]
    filter_value = -float("Inf")
    device = next(model.parameters()).device

    with torch.no_grad():

        for entry_idx in trange(entry_count):
            if embed is not None:
                generated = embed
            else:
                if tokens is None:
                    tokens = torch.tensor(tokenizer.encode(prompt))
                    tokens = tokens.unsqueeze(0).to(device)

                generated = model.gpt.transformer.wte(tokens)

            for i in range(entry_length):

                outputs = model.gpt(inputs_embeds=generated)
                logits = outputs.logits
                logits = logits[:, -1, :] / (temperature if temperature > 0 else 1.0)
                sorted_logits, sorted_indices = torch.sort(logits, descending=True)
                cumulative_probs = torch.cumsum(nnf.softmax(sorted_logits, dim=-1), dim=-1)
                sorted_indices_to_remove = cumulative_probs > top_p
                sorted_indices_to_remove[..., 1:] = sorted_indices_to_remove[
                                                    ..., :-1
                                                    ].clone()
                sorted_indices_to_remove[..., 0] = 0

                indices_to_remove = sorted_indices[sorted_indices_to_remove]
                logits[:, indices_to_remove] = filter_value
                next_token = torch.argmax(logits, -1).unsqueeze(0)
                next_token_embed = model.gpt.transformer.wte(next_token)
                if tokens is None:
                    tokens = next_token
                else:
                    tokens = torch.cat((tokens, next_token), dim=1)
                generated = torch.cat((generated, next_token_embed), dim=1)
                if stop_token_index == next_token.item():
                    break

            output_list = list(tokens.squeeze().cpu().numpy())
            output_text = tokenizer.decode(output_list)
            generated_list.append(output_text)

    return generated_list[0]
    
    
is_gpu = True 
device = CUDA(0) if is_gpu else "cpu"
clip_model, preprocess = clip.load("ViT-B/32", device=device, jit=False)
tokenizer = GPT2Tokenizer.from_pretrained("gpt2")


def inference(img):
    print("start inference")
    prefix_length = 10
    model = ClipCaptionModel(prefix_length)
    model_path = coco_weight
    model.load_state_dict(torch.load(model_path, map_location=CPU)) 
    model = model.eval() 
    device = CUDA(0) if is_gpu else "cpu"
    
    print(device)
    model = model.to(device)

    use_beam_search = True 
    image = io.imread(img)
    pil_image = PIL.Image.fromarray(image)  
    image = preprocess(pil_image).unsqueeze(0).to(device)
    with torch.no_grad():
        prefix = clip_model.encode_image(image).to(device, dtype=torch.float32)
        prefix_embed = model.clip_project(prefix).reshape(1, prefix_length, -1)
    if use_beam_search:
        generated_text_prefix = generate_beam(model, tokenizer, embed=prefix_embed)[0]
    else:
        generated_text_prefix = generate2(model, tokenizer, embed=prefix_embed)
    return generated_text_prefix

# file_path = sys.argv[1]
# file_path = "elephant.png"
# print(inference(file_path))

app = Flask(__name__)


@app.route("/")
def hello():
    return "Hello Flask"


# upload an image file
@app.route("/imagecaption", methods=['POST'])
def image_caption():
    if (request.method == 'POST'):
        f = request.files['file']
        
        ######################## SELECT #######################
        
        ##### (1) GPU Server 폴더 구조
        f.save('server/static/imagecaption/' + f.filename)
        result = inference('server/static/imagecaption/' + f.filename)
        
        ##### (2) Local Server 폴더 구조
        # f.save('./static/imagecaption/' + f.filename)
        # result = inference('./static/imagecaption/' + f.filename)
        
        #######################################################
        return result
    
    
@app.route("/logodetect", methods=['POST'])
def logo_detect():
    if (request.method == 'POST'):
        f = request.files['file']
        
        ######################## SELECT #######################
        
        ##### (1) GPU Server 폴더 구조
        f.save('server/static/logodetect' + f.filename)
        result = logodetect('server/static/logodetect' + f.filename)
        
        ##### (2) Local Server 폴더 구조
        # f.save('./static/logodetect/' + f.filename)
        # result = logodetect('./static/logodetect/' + f.filename)
        
        #######################################################
        return result
    
    
# returns a piece of data in JSON format
@app.route("/people")
def people():
    people = {"alice":25, "jin":35}
    return jsonify(people)


# returns an HTML webpage
@app.route("/user/<username>")
def user(username):
    return render_template('profile.html', name=username)


# send URL to get an image captioning result
@app.route("/s3/imagecaption", methods=['POST'])
def s3_image_caption():
    req = request.json
    address = req['address']
    result = inference(address)
    return result


# send URL to get an logo detection result
@app.route("/s3/logodetect", methods=['POST'])
def s3_logo_detection():
    req = request.json
    address = req['address']
    result = logodetect(address)
    return result


# run was
######################## SELECT #######################

##### (1) GPU Server
app.run(host='70.12.130.121', port='5000', debug=True)

##### (2) Local Server
# app.run(port='5000', debug=True)

#######################################################