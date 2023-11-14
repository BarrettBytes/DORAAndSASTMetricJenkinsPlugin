__author__ = 'marble_xu'

import os
import json
from abc import abstractmethod
import pygame as pg
from . import constants as c

from PIL import Image
import requests
import cv2
import numpy as np
import random
import time

class State():
    def __init__(self):
        self.start_time = 0.0
        self.current_time = 0.0
        self.done = False
        self.next = None
        self.persist = {}
    
    @abstractmethod
    def startup(self, current_time, persist):
        '''abstract method'''

    def cleanup(self):
        self.done = False
        return self.persist
    
    @abstractmethod
    def update(self, surface, keys, current_time):
        '''abstract method'''

class Control():
    def __init__(self):
        self.screen = pg.display.get_surface()
        self.done = False
        self.clock = pg.time.Clock()
        self.fps = 60
        self.keys = pg.key.get_pressed()
        self.mouse_pos = None
        self.mouse_click = [False, False]  # value:[left mouse click, right mouse click]
        self.current_time = 0.0
        self.state_dict = {}
        self.state_name = None
        self.state = None
        self.game_info = {c.CURRENT_TIME:0.0,
                          c.LEVEL_NUM:c.START_LEVEL_NUM}
 
    def setup_states(self, state_dict, start_state):
        self.state_dict = state_dict
        self.state_name = start_state
        self.state = self.state_dict[self.state_name]
        self.state.startup(self.current_time, self.game_info)

    def update(self):
        self.current_time = pg.time.get_ticks()
        if self.state.done:
            self.flip_state()
        self.state.update(self.screen, self.current_time, self.mouse_pos, self.mouse_click)
        self.mouse_pos = None
        self.mouse_click[0] = False
        self.mouse_click[1] = False

    def flip_state(self):
        previous, self.state_name = self.state_name, self.state.next
        persist = self.state.cleanup()
        self.state = self.state_dict[self.state_name]
        self.state.startup(self.current_time, persist)


    def playMouseClickMusic(self):
        # Define the path to the mp3 file
        mp3_path = os.path.join('resources','Bgm','mouse_click.mp3')
        # Initialize the mixer (if not already done)
        pg.mixer.init()
        # Load background music
        mouse_music = pg.mixer.Sound(mp3_path)

        # Play the background music (play it once)
        mouse_music.play()

    def event_loop(self):
        for event in pg.event.get():
            if event.type == pg.QUIT:
                self.done = True
            elif event.type in (pg.KEYDOWN, pg.KEYUP):
                self.keys = pg.key.get_pressed()
            elif event.type == pg.MOUSEBUTTONDOWN:
                self.mouse_pos = pg.mouse.get_pos()
                self.mouse_click[0], _, self.mouse_click[1] = pg.mouse.get_pressed()
                print('pos:', self.mouse_pos, ' mouse:', self.mouse_click)
                self.playMouseClickMusic()
                
    def main(self):
        while not self.done:
            self.event_loop()
            self.update()
            pg.display.update()
            self.clock.tick(self.fps)
        print('game over')

def get_image(sheet, x, y, width, height, colorkey=c.BLACK, scale=1):
        image = pg.Surface([width, height])
        rect = image.get_rect()

        image.blit(sheet, (0, 0), (x, y, width, height))
        image.set_colorkey(colorkey)
        image = pg.transform.scale(image,
                                   (int(rect.width*scale),
                                    int(rect.height*scale)))
        return image

def load_image_frames(directory, image_name, colorkey, accept):
    frame_list = []
    
    index_start = len(image_name) + 1 
    for pic in sorted(os.listdir(directory)):
        name, ext = os.path.splitext(pic)
        if ext.lower() in accept:
            img = pg.image.load(os.path.join(directory, pic))
            if img.get_alpha():
                img = img.convert_alpha()
            else:
                img = img.convert()
                img.set_colorkey(colorkey)
            frame_list.append(img)
            
    return frame_list


def load_all_gfx(directory, colorkey=c.WHITE, accept=('.png', '.jpg', '.bmp', '.gif')):
    graphics = {}

    # Randomly select one of the AI images to be the main menu each time the game is run
    # Get the current time as an integer (e.g., seconds since the epoch)
    current_time = int(time.time())

    # Set the seed based on the current time
    random.seed(current_time)

    # Generate a random number between 1 and 4 (inclusive)
    random_number = random.randint(1, 4)

    # Location of the randomly selected image
    AI_image_location = os.path.join(directory, 'Screen', 'AI', str(random_number) + '.png')

    # Open the chosen image
    input_image = Image.open(AI_image_location)

    # Define the new dimensions (width and height) for the compressed image
    new_width = 800  # Set your desired width
    new_height = 600  # Set your desired height

    # Resize the image to the new dimensions
    compressed_image = input_image.resize((new_width, new_height))
    
    # Save the compressed image
    # Specify the local file path where you want to save the image
    local_file_path = os.path.join(directory, 'Screen', 'MainMenu.png')

    # Save the cropped image to a file
    compressed_image.save(local_file_path)
    
    """
    # URL of the image you want to download
    image_url = "https://cdn.mos.cms.futurecdn.net/88298011cbb1b92aa9d2c1c7d9c1edf3.jpg"

    # Send an HTTP GET request to the image URL
    response = requests.get(image_url)

    if response.status_code == 200:
        # Get the content of the response (the image data)
        image_data = response.content

        # Specify the local file path where you want to save the image
        local_file_path = os.path.join(directory, 'Screen', 'MainMenu.jpg')

        # Write the image data to the local file
        with open(local_file_path, "wb") as f:
            f.write(image_data)
    """
    
    # Load First Minecraft button in
    # URL of the image you want to download
    image_url = "https://static.wikia.nocookie.net/minecraft_gamepedia/images/0/0a/World_1.19.4-pre3.png/revision/latest/scale-to-width-down/854?cb=20230302113338"

    # Send an HTTP GET request to the image URL
    response = requests.get(image_url)

    # Specify the local file path where you want to save the image
    local_file_path = os.path.join(directory, 'Screen', 'Adventure_0_uncropped.jpg')

    if response.status_code == 200:
        # Get the content of the response (the image data)
        image_data = response.content

        # Write the image data to the local file
        with open(local_file_path, "wb") as f:
            f.write(image_data)
            
    # Open the image
    input_image = Image.open(local_file_path)

    # Define the coordinates of the cropping box (left, upper, right, lower)
    crop_box = (118, 425, 415, 462)  # Adjust these coordinates as needed

    # Use the crop method to extract the specified region
    cropped_image = input_image.crop(crop_box)

    # Define the new dimensions (width and height) for the compressed image
    new_width = 200  # Set your desired width
    new_height = 40  # Set your desired height

    # Resize the image to the new dimensions
    cropped_image = cropped_image.resize((new_width, new_height))

    # Specify the local file path where you want to save the image
    local_file_path = os.path.join(directory, 'Screen', 'Adventure_0.jpg')

    # Save the cropped image to a file
    cropped_image.save(local_file_path)

    # Close the image files
    input_image.close()
    cropped_image.close()

    # Change elements of the minecraft button to blue when it is clicked
    # Load an image from a file
    image = cv2.imread(local_file_path)

    # Convert the image to grayscale
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Define the lower and upper bounds for gray in BGR format
    lower_gray = np.array([100, 100, 100], dtype=np.uint8)
    upper_gray = np.array([200, 200, 200], dtype=np.uint8)

    # Create a mask for the gray regions
    mask = cv2.inRange(image, lower_gray, upper_gray)

    # Change the color of the selected regions to blue (in BGR format)
    image[mask > 0] = [255, 0, 0]  # Blue
    
    # Specify the local file path where you want to save the image
    local_file_path = os.path.join(directory, 'Screen', 'Adventure_1.jpg')

    cv2.imwrite(local_file_path, image)


    for name1 in os.listdir(directory):
        # subfolders under the folder resources\graphics
        dir1 = os.path.join(directory, name1)
        if os.path.isdir(dir1):
            for name2 in os.listdir(dir1):
                dir2 = os.path.join(dir1, name2)
                if os.path.isdir(dir2):
                # e.g. subfolders under the folder resources\graphics\Zombies
                    for name3 in os.listdir(dir2):
                        dir3 = os.path.join(dir2, name3)
                        # e.g. subfolders or pics under the folder resources\graphics\Zombies\ConeheadZombie
                        if os.path.isdir(dir3):
                            # e.g. it's the folder resources\graphics\Zombies\ConeheadZombie\ConeheadZombieAttack
                            image_name, _ = os.path.splitext(name3)
                            graphics[image_name] = load_image_frames(dir3, image_name, colorkey, accept)
                        else:
                            # e.g. pics under the folder resources\graphics\Plants\Peashooter
                            image_name, _ = os.path.splitext(name2)
                            graphics[image_name] = load_image_frames(dir2, image_name, colorkey, accept)
                            break
                else:
                # e.g. pics under the folder resources\graphics\Screen
                    name, ext = os.path.splitext(name2)
                    if ext.lower() in accept:
                        img = pg.image.load(dir2)
                        if img.get_alpha():
                            img = img.convert_alpha()
                        else:
                            img = img.convert()
                            img.set_colorkey(colorkey)
                        graphics[name] = img
    return graphics

def loadZombieImageRect():
    file_path = os.path.join('source', 'data', 'entity', 'zombie.json')
    f = open(file_path)
    data = json.load(f)
    f.close()
    return data[c.ZOMBIE_IMAGE_RECT]

def loadPlantImageRect():
    # Define the path to the mp3 file
    mp3_path = os.path.join('resources', 'Bgm', 'homePage_bgm.mp3')

    # Initialize the mixer (if not already done)
    pg.mixer.init()

    # Load background music
    background_music = pg.mixer.Sound(mp3_path)

    # Play the background music (loop indefinitely)
    background_music.play(-1)

    # Load plant image rectangles
    file_path = os.path.join('source', 'data', 'entity', 'plant.json')
    f = open(file_path)
    data = json.load(f)
    f.close()
    return data[c.PLANT_IMAGE_RECT]


pg.init()
pg.display.set_caption(c.ORIGINAL_CAPTION)
SCREEN = pg.display.set_mode(c.SCREEN_SIZE)

GFX = load_all_gfx(os.path.join("resources","graphics"))
ZOMBIE_RECT = loadZombieImageRect()
PLANT_RECT = loadPlantImageRect()
