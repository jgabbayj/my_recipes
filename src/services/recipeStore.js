const DEFAULT_RECIPES = [
  {
    id: "spaghetti-carbonara",
    title: "Classic Spaghetti Carbonara",
    description: "An authentic Roman pasta dish made with fresh eggs, hard cheese, cured pork, and black pepper. Rich, creamy, and ready in 20 minutes.",
    image: "https://images.unsplash.com/photo-1612874742237-6526221588e3?auto=format&fit=crop&w=800&q=80",
    servings: 4,
    prepTime: 5,
    cookTime: 15,
    difficulty: "Medium",
    category: "Dinner",
    ingredients: [
      "350g Spaghetti",
      "150g Guanciale or Pancetta (cubed)",
      "4 Large Fresh Egg Yolks",
      "1 Whole Large Fresh Egg",
      "75g Pecorino Romano (grated)",
      "50g Parmigiano Reggiano (grated)",
      "2 tsp Coarsely Ground Black Pepper",
      "1 tsp Sea Salt (for boiling water)"
    ],
    steps: [
      "Bring a large pot of salted water to a rolling boil. Add the spaghetti and cook according to package instructions until al dente (about 8-10 minutes).",
      "While the pasta cooks, heat a large pan over medium heat. Add the cubed guanciale (or pancetta) and cook until it is crispy and the fat has rendered (about 5-7 minutes). Remove the pan from heat.",
      "In a medium bowl, whisk together the 4 egg yolks, 1 whole egg, Pecorino Romano, and Parmigiano Reggiano. Season generously with the ground black pepper to form a thick paste.",
      "Just before draining the pasta, ladle out about 1 cup (240ml) of the starchy pasta cooking water and set it aside.",
      "Drain the pasta and immediately add it to the pan with the warm guanciale and its rendered fat. Toss well for 30 seconds to coat the pasta and let it cool slightly (so the eggs don't scramble).",
      "Pour the egg and cheese mixture over the pasta. Toss rapidly. Add 1/4 cup of the reserved pasta water and keep tossing. The heat from the pasta will cook the eggs gently, creating a smooth, creamy sauce. Add more pasta water if it's too thick.",
      "Serve immediately in warm bowls, topped with extra grated Pecorino Romano and freshly cracked black pepper."
    ]
  },
  {
    id: "chocolate-chip-cookies",
    title: "Ultimate Chocolate Chip Cookies",
    description: "Crispy on the edges, soft and chewy in the center, loaded with rich chocolate pools and a hint of sea salt. The ultimate comfort treat.",
    image: "https://images.unsplash.com/photo-1499636136210-6f4ee915583e?auto=format&fit=crop&w=800&q=80",
    servings: 12,
    prepTime: 15,
    cookTime: 10,
    difficulty: "Easy",
    category: "Dessert",
    ingredients: [
      "225g Unsalted Butter (browned and cooled)",
      "150g Dark Brown Sugar",
      "100g Granulated White Sugar",
      "2 Large Eggs (room temperature)",
      "2 tsp Vanilla Extract",
      "325g All-Purpose Flour",
      "1 tsp Baking Soda",
      "3/4 tsp Fine Sea Salt",
      "200g Dark Chocolate Discs or Chunks (70% cocoa)",
      "Flaky Sea Salt (for topping)"
    ],
    steps: [
      "Melt the butter in a saucepan over medium heat, swirling frequently. Cook until the butter turns a deep golden amber color and smells nutty (about 5 minutes). Pour into a bowl and let it cool completely.",
      "In a large bowl, whisk the cooled brown butter with the brown sugar and white sugar until well combined and no lumps remain.",
      "Add the eggs one at a time, whisking vigorously after each addition, then whisk in the vanilla extract. Beat the mixture for 1-2 minutes until it becomes pale and slightly thickened.",
      "In a separate bowl, sift the flour, baking soda, and fine sea salt together. Fold these dry ingredients into the wet ingredients using a spatula just until a dough starts to form (do not overmix).",
      "Gently fold in the dark chocolate chunks, reserving a few to press on top of the cookies before baking.",
      "Cover the cookie dough and chill in the refrigerator for at least 30 minutes (or up to 24 hours for deeper flavor).",
      "Preheat your oven to 180°C (350°F) and line two large baking sheets with parchment paper.",
      "Scoop out large balls of dough (about 3 tablespoons each) and place them 2 inches apart on the baking sheets. Press a few reserved chocolate chunks on top.",
      "Bake for 9-11 minutes, until the edges are golden brown but the centers are still soft and slightly pale.",
      "Remove from the oven, immediately sprinkle with a pinch of flaky sea salt, and let them cool on the baking sheet for 5 minutes before transferring to a wire rack."
    ]
  },
  {
    id: "avocado-toast",
    title: "Premium Avocado & Poached Egg Toast",
    description: "Thick slice of artisanal sourdough, toasted to perfection, topped with creamy mashed avocado, perfectly poached egg, chili flakes, and microgreens.",
    image: "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=800&q=80",
    servings: 1,
    prepTime: 5,
    cookTime: 5,
    difficulty: "Easy",
    category: "Breakfast",
    ingredients: [
      "1 Thick Slice Sourdough Bread",
      "1 Ripe Hass Avocado",
      "1 Large Fresh Egg",
      "1 tbsp Fresh Lemon Juice",
      "1 tbsp Extra Virgin Olive Oil",
      "1/2 tsp Red Chili Flakes",
      "1/2 tsp Everything Bagel Seasoning",
      "1 tsp Apple Cider Vinegar (for poaching)",
      "Salt and Freshly Cracked Black Pepper to taste",
      "Fresh Microgreens (for garnish)"
    ],
    steps: [
      "Toast the sourdough slice in a toaster or griddle until golden brown and crispy on both sides. Brush lightly with extra virgin olive oil.",
      "Cut the avocado in half, remove the pit, and scoop the flesh into a small bowl. Add the lemon juice, a drizzle of olive oil, salt, and black pepper. Mash with a fork until chunky-smooth.",
      "Bring a small pot of water to a gentle simmer (not a boil). Add the apple cider vinegar. Crack the egg into a small ramekin.",
      "Create a gentle whirlpool in the simmering water using a spoon, and gently slip the egg into the center of the whirlpool. Poach for 3 minutes for a runny yolk.",
      "Using a slotted spoon, carefully remove the poached egg from the water and drain it on a paper towel.",
      "Spread the mashed avocado generously over the toasted sourdough slice.",
      "Place the poached egg gently on top of the avocado bed.",
      "Garnish with red chili flakes, everything bagel seasoning, extra cracked black pepper, and fresh microgreens. Serve immediately!"
    ]
  }
];

export const recipeStore = {
  getAll: () => {
    const data = localStorage.getItem("my_recipes");
    if (!data) {
      localStorage.setItem("my_recipes", JSON.stringify(DEFAULT_RECIPES));
      return DEFAULT_RECIPES;
    }
    try {
      return JSON.parse(data);
    } catch (e) {
      console.error("Failed to parse recipes from localStorage", e);
      return DEFAULT_RECIPES;
    }
  },

  saveAll: (recipes) => {
    localStorage.setItem("my_recipes", JSON.stringify(recipes));
  },

  getById: (id) => {
    const recipes = recipeStore.getAll();
    return recipes.find(r => r.id === id);
  },

  add: (recipe) => {
    const recipes = recipeStore.getAll();
    const newRecipe = {
      ...recipe,
      id: recipe.id || `recipe-${Date.now()}`
    };
    recipes.unshift(newRecipe);
    recipeStore.saveAll(recipes);
    return newRecipe;
  },

  update: (id, updatedFields) => {
    const recipes = recipeStore.getAll();
    const index = recipes.findIndex(r => r.id === id);
    if (index !== -1) {
      recipes[index] = { ...recipes[index], ...updatedFields };
      recipeStore.saveAll(recipes);
      return recipes[index];
    }
    return null;
  },

  delete: (id) => {
    const recipes = recipeStore.getAll();
    const filtered = recipes.filter(r => r.id !== id);
    recipeStore.saveAll(filtered);
    return filtered;
  }
};
