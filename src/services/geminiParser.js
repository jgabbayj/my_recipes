import { GoogleGenerativeAI } from '@google/generative-ai';

// Mock recipes for demonstration when API key is not present or for test URLs
const MOCK_PARSED_RECIPES = {
  "lasagna": {
    title: "Best Ever Homemade Lasagna",
    description: "Classic lasagna made with a rich meat sauce, creamy ricotta mixture, and gooey mozzarella cheese. Layered to perfection and baked until bubbly.",
    servings: 8,
    prepTime: 30,
    cookTime: 50,
    difficulty: "Hard",
    category: "Dinner",
    ingredients: [
      "12 Lasagna Noodles",
      "500g Ground Beef",
      "250g Italian Sausage",
      "1 Medium Onion (chopped)",
      "2 Cloves Garlic (minced)",
      "800g Crushed Tomatoes",
      "2 tbsp Tomato Paste",
      "2 tsp Dried Oregano",
      "2 tsp Dried Basil",
      "450g Ricotta Cheese",
      "1 Egg",
      "50g Parmesan Cheese (grated)",
      "400g Mozzarella Cheese (shredded)"
    ],
    steps: [
      "Cook the lasagna noodles in a large pot of boiling salted water according to package directions. Drain and rinse with cold water.",
      "In a large pan, cook the ground beef, sausage, onion, and garlic over medium-high heat until browned. Drain excess fat.",
      "Stir in the crushed tomatoes, tomato paste, dried oregano, dried basil, salt, and pepper. Simmer uncovered for 15-20 minutes, stirring occasionally.",
      "In a bowl, mix together the ricotta cheese, egg, half of the Parmesan cheese, and a pinch of salt.",
      "Preheat oven to 190°C (375°F).",
      "To assemble, spread 1 cup of meat sauce in a 9x13 inch baking dish. Arrange 4 noodles on top. Spread half of the ricotta mixture, then sprinkle with 1/3 of the mozzarella cheese. Repeat layering with sauce, noodles, ricotta, and mozzarella.",
      "Finish with a final layer of noodles, the remaining meat sauce, and the rest of the mozzarella and Parmesan cheese.",
      "Cover with foil (raised slightly so it doesn't touch the cheese) and bake for 25 minutes. Remove foil and bake for an additional 25 minutes until bubbly and golden brown."
    ]
  },
  "tacos": {
    title: "Street-Style Beef Tacos",
    description: "Quick and flavorful street tacos with seasoned ground beef, fresh onions, cilantro, and lime juice on warm corn tortillas.",
    servings: 4,
    prepTime: 10,
    cookTime: 10,
    difficulty: "Easy",
    category: "Lunch",
    ingredients: [
      "500g Ground Beef",
      "1 packet Taco Seasoning",
      "1/2 cup Water",
      "12 Small Corn Tortillas",
      "1 small White Onion (finely diced)",
      "1/2 cup Fresh Cilantro (chopped)",
      "2 Limes (cut into wedges)",
      "Salsa or Hot Sauce to taste"
    ],
    steps: [
      "Brown the ground beef in a large skillet over medium-high heat until fully cooked. Drain excess fat.",
      "Add the taco seasoning and 1/2 cup water. Bring to a simmer and cook for 3-5 minutes until the sauce thickens and coats the beef.",
      "Warm the corn tortillas on a dry skillet or griddle over medium heat for about 30 seconds on each side until pliable.",
      "Assemble the tacos by placing 2-3 tablespoons of beef in each tortilla.",
      "Top with diced onion, fresh cilantro, a squeeze of fresh lime juice, and salsa. Serve warm."
    ]
  }
};

export const geminiParser = {
  /**
   * Parses recipe data from a URL using Gemini AI, falling back to mock data if no key or if it matches mock triggers.
   */
  parseFromUrl: async (url, apiKey) => {
    // Check if url matches mock patterns
    const urlLower = url.toLowerCase();
    let mockKey = null;
    if (urlLower.includes("lasagna")) mockKey = "lasagna";
    else if (urlLower.includes("taco")) mockKey = "tacos";
    
    // If it's a mock URL or we don't have an API key, use mock parsing for demo
    if (mockKey || !apiKey) {
      // Simulate network delay
      await new Promise(resolve => setTimeout(resolve, 2000));
      if (mockKey && MOCK_PARSED_RECIPES[mockKey]) {
        return {
          success: true,
          recipe: MOCK_PARSED_RECIPES[mockKey],
          message: "Recipe parsed successfully using simulator."
        };
      }
      // If we don't have an API key, we cannot make the real call, so return the lasagna one as a demo
      return {
        success: true,
        recipe: MOCK_PARSED_RECIPES["lasagna"],
        message: "No Gemini API key provided. Loaded demo lasagna recipe."
      };
    }

    try {
      // Fetch the page content. We try to go through our local proxy endpoint
      let htmlContent = "";
      try {
        const response = await fetch(`/api/fetch-url?url=${encodeURIComponent(url)}`);
        if (response.ok) {
          const data = await response.json();
          htmlContent = data.text;
        } else {
          throw new Error("Proxy fetch failed");
        }
      } catch (proxyError) {
        console.warn("Proxy fetch failed, attempting client-side scrape or asking for manual input", proxyError);
        // Fallback: We can't fetch because of CORS, so we will tell the caller to request text copy-paste
        return {
          success: false,
          error: "CORS_RESTRICTION",
          message: "Unable to fetch the webpage directly due to browser security restrictions (CORS). Please copy the recipe page content and paste it in the text box below!"
        };
      }

      return await geminiParser.parseFromText(htmlContent, apiKey);
    } catch (error) {
      console.error("Failed to parse recipe from URL", error);
      return {
        success: false,
        error: error.message || "Unknown error",
        message: "Failed to parse recipe. Please make sure the URL is correct and contains a recipe."
      };
    }
  },

  /**
   * Parses recipe data from a raw text or HTML block using Gemini AI
   */
  parseFromText: async (text, apiKey) => {
    if (!apiKey) {
      return {
        success: false,
        error: "NO_API_KEY",
        message: "Please enter your Gemini API Key in Settings to use the automatic parser."
      };
    }

    try {
      const genAI = new GoogleGenerativeAI(apiKey);
      const model = genAI.getGenerativeModel({
        model: "gemini-1.5-flash",
        generationConfig: {
          responseMimeType: "application/json",
        }
      });

      const prompt = `
        You are an expert recipe parser. Your task is to extract recipe information from the provided text or HTML content and return it as a structured JSON object.
        
        The text or HTML content is:
        -----------------
        ${text.slice(0, 30000)}  // Limit to 30k characters to prevent token overflow
        -----------------

        Please extract the following fields and return exactly this JSON schema:
        {
          "title": "Title of the recipe (string)",
          "description": "A brief summary of the recipe (string)",
          "image": "URL of the recipe main image if found, otherwise leave empty or use a placeholder (string)",
          "servings": "Number of servings as an integer, default to 4 if not found (number)",
          "prepTime": "Preparation time in minutes as an integer, default to 10 if not found (number)",
          "cookTime": "Cooking time in minutes as an integer, default to 20 if not found (number)",
          "difficulty": "One of: 'Easy', 'Medium', 'Hard'",
          "category": "One of: 'Breakfast', 'Lunch', 'Dinner', 'Dessert', 'Snack', 'Other'",
          "ingredients": [
            "List of ingredients with quantities, e.g., '2 cups flour', '1 tsp salt' (array of strings)"
          ],
          "steps": [
            "Step-by-step instructions. Break down long paragraphs into clear, single actions. Ensure they are in sequential order (array of strings)"
          ]
        }

        Make sure you only return the raw JSON object, no Markdown markdown wrappers, no comments.
      `;

      const result = await model.generateContent(prompt);
      const responseText = result.response.text();
      
      try {
        const recipeData = JSON.parse(responseText.trim());
        
        // Basic validation and sanitization
        const sanitizedRecipe = {
          title: recipeData.title || "Parsed Recipe",
          description: recipeData.description || "Parsed using Gemini AI",
          image: recipeData.image || "https://images.unsplash.com/photo-1495521821757-a1efb6729352?auto=format&fit=crop&w=800&q=80",
          servings: parseInt(recipeData.servings) || 4,
          prepTime: parseInt(recipeData.prepTime) || 10,
          cookTime: parseInt(recipeData.cookTime) || 20,
          difficulty: ['Easy', 'Medium', 'Hard'].includes(recipeData.difficulty) ? recipeData.difficulty : 'Medium',
          category: ['Breakfast', 'Lunch', 'Dinner', 'Dessert', 'Snack', 'Other'].includes(recipeData.category) ? recipeData.category : 'Dinner',
          ingredients: Array.isArray(recipeData.ingredients) ? recipeData.ingredients : [],
          steps: Array.isArray(recipeData.steps) ? recipeData.steps : []
        };

        return {
          success: true,
          recipe: sanitizedRecipe,
          message: "Recipe parsed successfully using Gemini AI!"
        };
      } catch (parseError) {
        console.error("Failed to parse Gemini response as JSON. Response was:", responseText, parseError);
        return {
          success: false,
          error: "JSON_PARSE_ERROR",
          message: "The AI returned a response, but it could not be parsed as structured recipe data. Please try copy-pasting the text instead."
        };
      }
    } catch (error) {
      console.error("Gemini API call failed", error);
      return {
        success: false,
        error: error.message || "API_ERROR",
        message: `Gemini API error: ${error.message || "Please check your API key and network connection."}`
      };
    }
  }
};
