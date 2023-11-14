__author__ = 'marble_xu'

from matplotlib.offsetbox import AnnotationBbox, OffsetImage
import matplotlib.pyplot as plt
import networkx as nx
import numpy as np
from source import tool
from source import constants as c
from source.state import mainmenu, screen, level

def main():
    game = tool.Control()
    state_dict = {c.MAIN_MENU: mainmenu.Menu(),
                  c.GAME_VICTORY: screen.GameVictoryScreen(),
                  c.GAME_LOSE: screen.GameLoseScreen(),
                  c.LEVEL: level.Level()}
    game.setup_states(state_dict, c.MAIN_MENU)
    add_class_map()
    game.main()

# Also, let's add a class map of classes used in the game
import inspect
import networkx as nx
import matplotlib.pyplot as plt
import importlib.util
import pygame as pg
import os
import glob
import sys

def load_module_from_path(path):
    """Dynamically load a module from a given file path."""
    import sys
    module_dir = os.path.dirname(path)
    module_name = os.path.splitext(os.path.basename(path))[0]

    if module_dir not in sys.path:
        sys.path.insert(0, module_dir)

    module = importlib.import_module(module_name)
    return module

def add_class_map():
    # Load the module from the specified file path
    import os

    current_directory = os.path.dirname(os.path.abspath(__file__))
    parent_directory = os.path.dirname(current_directory)

    # Load the plant module
    plant_path = os.path.join(current_directory, "component", "plant.py")
    plant_module = load_module_from_path(plant_path)
    plant_classes = extract_game_classes(plant_module, pg.sprite.Sprite)

    # Load the zombie module
    zombie_path = os.path.join(current_directory, "component", "zombie.py")
    zombie_module = load_module_from_path(zombie_path)
    zombie_classes = extract_game_classes(zombie_module, pg.sprite.Sprite)

    #image path
    image_path = os.path.join(parent_directory, "resources/PlantsVsZombiesImages")
    nodeimage_path = os.path.join(parent_directory, "resources\graphics")



    # Combine both plant and zombie classes
    game_classes = plant_classes + zombie_classes

    relationships = extract_relationships(game_classes)
    visualize_class_hierarchy(relationships, image_path, nodeimage_path)

def extract_game_classes(module, base_class):
    """Extract classes from the module that inherit from the specified base class."""
    all_classes = inspect.getmembers(module, inspect.isclass)
    return [cls for name, cls in all_classes if issubclass(cls, base_class) and cls != base_class]

def extract_relationships(classes):
    """Extract inheritance relationships between classes."""
    relationships = {}
    for cls in classes:
        base_classes = [base for base in inspect.getmro(cls) if base in classes]
        relationships[cls] = base_classes[1:]  # Exclude itself
    return relationships

import os
import random

def visualize_class_hierarchy(relationships, background_image_folder, nodeimage_path):
    G = nx.DiGraph()
    for cls, bases in relationships.items():
        for base in bases:
            G.add_edge(cls.__name__, base.__name__)

    fig, ax = plt.subplots(figsize=(10, 6))

    # If a background image folder is provided, randomly select and display an image from the folder
    if background_image_folder:
        image_files = [f for f in os.listdir(background_image_folder) if f.lower().endswith(('.png', '.jpg', '.jpeg'))]
        random_image_path = os.path.join(background_image_folder, random.choice(image_files))
        img = plt.imread(random_image_path)
        ax.imshow(img, aspect='auto', extent=[-1.5, 1.5, -1.5, 1.5])

    pos = nx.spring_layout(G, k=4, iterations=100)

    # Draw edges
    nx.draw_networkx_edges(G, pos, ax=ax)

    # Overlay images on the nodes
    for node in G.nodes():
        matching_files = [f for f in glob.glob(os.path.join(nodeimage_path, "**", f"*{node}*.png"), recursive=True) if "Cards" not in f]

        
        if matching_files:
            img = plt.imread(matching_files[0])
            tmp = img[:, :, :3]
            white = np.array([1, 1, 1])
            mask = np.abs(tmp - white).sum(axis=2) < 0.05
            img[mask, 3] = 0
        else:
            if "Zombie" in node:

                zombie_images = glob.glob(os.path.join(nodeimage_path, "Zombies/NormalZombie","**", "*.png"), recursive=True)
                img = plt.imread(random.choice(zombie_images))  # Randomly select a zombie image
            else:
                plant_images = glob.glob(os.path.join(nodeimage_path, "Plants/Peashooter", "**","*.png"), recursive=True)
                img = plt.imread(random.choice(plant_images))  # Randomly select a plant image
        
        imagebox = OffsetImage(img, zoom=0.8)
        ab = AnnotationBbox(imagebox, pos[node], frameon=False, boxcoords="data", pad=0) # Ensure boxcoords is set to "data"
        ax.add_artist(ab)

        nx.draw_networkx_labels(G, pos, font_color='#90EE90', font_family='Comic Sans MS', ax=ax)


    manager = plt.get_current_fig_manager()
    try:
        manager.resize(*manager.window.maxsize())
    except Exception as e:
        manager.full_screen_toggle()
    plt.show()
