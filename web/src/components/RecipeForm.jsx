import React, { useState, useEffect } from 'react';
import { ArrowLeft, Plus, Trash2, Globe, Sparkles, Loader2, BookOpen, FileText } from 'lucide-react';
import { geminiParser } from '../services/geminiParser';

const CATEGORIES = ['Breakfast', 'Lunch', 'Dinner', 'Dessert', 'Snack', 'Other'];
const DIFFICULTIES = ['Easy', 'Medium', 'Hard'];

export default function RecipeForm({ recipe, onSave, onBack, apiKey }) {
  const [activeMode, setActiveMode] = useState('manual'); // 'manual' or 'parse'
  const isEditing = !!recipe;

  // Form Fields
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [image, setImage] = useState('');
  const [servings, setServings] = useState(4);
  const [prepTime, setPrepTime] = useState(10);
  const [cookTime, setCookTime] = useState(20);
  const [difficulty, setDifficulty] = useState('Medium');
  const [category, setCategory] = useState('Dinner');
  const [ingredients, setIngredients] = useState(['']);
  const [steps, setSteps] = useState(['']);

  // Parser States
  const [url, setUrl] = useState('');
  const [rawText, setRawText] = useState('');
  const [isParsing, setIsParsing] = useState(false);
  const [parseError, setParseError] = useState(null);
  const [isCorsRestricted, setIsCorsRestricted] = useState(false);

  // Initialize form if editing
  useEffect(() => {
    if (recipe) {
      setTitle(recipe.title || '');
      setDescription(recipe.description || '');
      setImage(recipe.image || '');
      setServings(recipe.servings || 4);
      setPrepTime(recipe.prepTime || 10);
      setCookTime(recipe.cookTime || 20);
      setDifficulty(recipe.difficulty || 'Medium');
      setCategory(recipe.category || 'Dinner');
      setIngredients(recipe.ingredients && recipe.ingredients.length > 0 ? recipe.ingredients : ['']);
      setSteps(recipe.steps && recipe.steps.length > 0 ? recipe.steps : ['']);
    }
  }, [recipe]);

  const handleAddIngredient = () => setIngredients([...ingredients, '']);
  const handleRemoveIngredient = (index) => {
    const next = ingredients.filter((_, idx) => idx !== index);
    setIngredients(next.length > 0 ? next : ['']);
  };
  const handleIngredientChange = (index, value) => {
    const next = [...ingredients];
    next[index] = value;
    setIngredients(next);
  };

  const handleAddStep = () => setSteps([...steps, '']);
  const handleRemoveStep = (index) => {
    const next = steps.filter((_, idx) => idx !== index);
    setSteps(next.length > 0 ? next : ['']);
  };
  const handleStepChange = (index, value) => {
    const next = [...steps];
    next[index] = value;
    setSteps(next);
  };

  const loadParsedDataIntoForm = (parsedRecipe) => {
    setTitle(parsedRecipe.title || '');
    setDescription(parsedRecipe.description || '');
    setImage(parsedRecipe.image || '');
    setServings(parsedRecipe.servings || 4);
    setPrepTime(parsedRecipe.prepTime || 10);
    setCookTime(parsedRecipe.cookTime || 20);
    setDifficulty(parsedRecipe.difficulty || 'Medium');
    setCategory(parsedRecipe.category || 'Dinner');
    setIngredients(parsedRecipe.ingredients || ['']);
    setSteps(parsedRecipe.steps || ['']);
  };

  const handleParseUrl = async () => {
    if (!url) return;
    setIsParsing(true);
    setParseError(null);
    setIsCorsRestricted(false);

    try {
      const result = await geminiParser.parseFromUrl(url, apiKey);
      if (result.success) {
        loadParsedDataIntoForm(result.recipe);
        setActiveMode('manual'); // Switch to manual form so user can review and edit
      } else if (result.error === 'CORS_RESTRICTION') {
        setIsCorsRestricted(true);
        setParseError(result.message);
      } else {
        setParseError(result.message);
      }
    } catch (err) {
      setParseError(err.message || 'Failed to parse recipe');
    } finally {
      setIsParsing(false);
    }
  };

  const handleParseText = async () => {
    if (!rawText) return;
    setIsParsing(true);
    setParseError(null);

    try {
      const result = await geminiParser.parseFromText(rawText, apiKey);
      if (result.success) {
        loadParsedDataIntoForm(result.recipe);
        setIsCorsRestricted(false);
        setActiveMode('manual'); // Switch to manual form for review
      } else {
        setParseError(result.message);
      }
    } catch (err) {
      setParseError(err.message || 'Failed to parse text');
    } finally {
      setIsParsing(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    // Filter out empty lines
    const cleanIngredients = ingredients.filter(i => i.trim() !== '');
    const cleanSteps = steps.filter(s => s.trim() !== '');

    if (!title.trim()) {
      alert("Please enter a recipe title");
      return;
    }
    if (cleanIngredients.length === 0) {
      alert("Please add at least one ingredient");
      return;
    }
    if (cleanSteps.length === 0) {
      alert("Please add at least one step");
      return;
    }

    onSave({
      id: recipe?.id,
      title,
      description,
      image: image.trim() || 'https://images.unsplash.com/photo-1495521821757-a1efb6729352?auto=format&fit=crop&w=800&q=80',
      servings: parseInt(servings) || 4,
      prepTime: parseInt(prepTime) || 10,
      cookTime: parseInt(cookTime) || 20,
      difficulty,
      category,
      ingredients: cleanIngredients,
      steps: cleanSteps
    });
  };

  return (
    <div className="recipe-form-view" style={{ display: 'flex', flexDirection: 'column', height: '100%', backgroundColor: 'var(--bg-mobile)' }}>
      {/* Header */}
      <div style={{ 
        padding: '16px 20px', 
        backgroundColor: 'var(--card-bg)', 
        borderBottom: '1px solid var(--border)',
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'space-between' 
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button 
            onClick={onBack}
            className="btn"
            style={{ padding: '8px', borderRadius: '50%', backgroundColor: 'transparent', color: 'var(--text-main)', border: 'none' }}
          >
            <ArrowLeft size={20} />
          </button>
          <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-main)', margin: 0 }}>
            {isEditing ? 'Edit Recipe' : 'Add New Recipe'}
          </h2>
        </div>
      </div>

      {/* Tabs for Add Modes (only when adding new, not editing) */}
      {!isEditing && (
        <div style={{ display: 'flex', borderBottom: '1px solid var(--border)', backgroundColor: 'var(--card-bg)' }}>
          <button 
            type="button"
            onClick={() => setActiveMode('manual')}
            style={{ 
              flex: 1, 
              padding: '14px 0', 
              fontSize: '14px', 
              fontWeight: 700, 
              color: activeMode === 'manual' ? 'var(--primary)' : 'var(--text-muted)',
              border: 'none',
              backgroundColor: 'transparent',
              borderBottom: activeMode === 'manual' ? '3px solid var(--primary)' : '3px solid transparent',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px'
            }}
          >
            <Plus size={16} />
            Manual Entry
          </button>
          <button 
            type="button"
            onClick={() => setActiveMode('parse')}
            style={{ 
              flex: 1, 
              padding: '14px 0', 
              fontSize: '14px', 
              fontWeight: 700, 
              color: activeMode === 'parse' ? 'var(--primary)' : 'var(--text-muted)',
              border: 'none',
              backgroundColor: 'transparent',
              borderBottom: activeMode === 'parse' ? '3px solid var(--primary)' : '3px solid transparent',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px'
            }}
          >
            <Sparkles size={16} style={{ color: 'var(--primary)' }} />
            Parse from Web / URL
          </button>
        </div>
      )}

      {/* Main Content Scroll Container */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '20px' }}>
        {activeMode === 'parse' ? (
          /* PARSING MODE PANEL */
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div style={{ backgroundColor: 'var(--card-bg)', padding: '20px', borderRadius: '20px', border: '1px solid var(--border)', boxShadow: 'var(--shadow-sm)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--primary)', marginBottom: '8px' }}>
                <Globe size={18} />
                <h3 style={{ fontSize: '15px', fontWeight: 700, margin: 0 }}>Import Web Recipe</h3>
              </div>
              <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '16px', lineHeight: '1.4' }}>
                Paste the link of any recipe web page. Gemini AI will automatically extract the ingredients, instructions, and save them in separate sections.
              </p>
              
              <div className="form-group" style={{ marginBottom: '14px' }}>
                <input
                  type="url"
                  placeholder="https://example.com/recipe-url"
                  value={url}
                  onChange={(e) => setUrl(e.target.value)}
                  className="form-input"
                  disabled={isParsing}
                  style={{ borderRadius: '12px' }}
                />
              </div>

              {!apiKey && (
                <div style={{ 
                  backgroundColor: 'rgba(224, 106, 59, 0.08)', 
                  border: '1px solid rgba(224, 106, 59, 0.2)', 
                  padding: '12px', 
                  borderRadius: '12px', 
                  marginBottom: '14px',
                  fontSize: '12px',
                  color: 'var(--primary)',
                  fontWeight: 600,
                  lineHeight: '1.4'
                }}>
                  ⚠️ No API Key set in Settings. Tapping parse will load a simulator demo recipe (e.g. lasagna). Add your API key in the top-right settings to run real queries.
                </div>
              )}

              <button
                type="button"
                onClick={handleParseUrl}
                disabled={isParsing || !url}
                className="btn btn-primary"
                style={{ width: '100%', borderRadius: '12px', padding: '12px', gap: '8px', opacity: (isParsing || !url) ? 0.6 : 1 }}
              >
                {isParsing ? (
                  <>
                    <Loader2 size={16} className="animate-spin" />
                    AI Parsing Webpage...
                  </>
                ) : (
                  <>
                    <Sparkles size={16} />
                    Extract Recipe Details
                  </>
                )}
              </button>
            </div>

            {/* CORS / Manual Text Parsing Fallback */}
            {isCorsRestricted && (
              <div style={{ backgroundColor: 'var(--card-bg)', padding: '20px', borderRadius: '20px', border: '1px solid var(--border)', boxShadow: 'var(--shadow-sm)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--primary)', marginBottom: '8px' }}>
                  <FileText size={18} />
                  <h3 style={{ fontSize: '15px', fontWeight: 700, margin: 0 }}>CORS Bypass: Paste Text</h3>
                </div>
                <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '14px', lineHeight: '1.4' }}>
                  The webpage is protected against automated fetching. You can open the recipe in another tab, copy all the page text (Ctrl+A, Ctrl+C), and paste it below!
                </p>
                
                <div className="form-group" style={{ marginBottom: '14px' }}>
                  <textarea
                    placeholder="Paste the copied recipe website content here..."
                    value={rawText}
                    onChange={(e) => setRawText(e.target.value)}
                    className="form-textarea"
                    disabled={isParsing}
                    style={{ minHeight: '140px', borderRadius: '12px' }}
                  />
                </div>

                <button
                  type="button"
                  onClick={handleParseText}
                  disabled={isParsing || !rawText}
                  className="btn btn-secondary"
                  style={{ width: '100%', borderRadius: '12px', padding: '12px', gap: '8px', border: '1px solid var(--primary)' }}
                >
                  {isParsing ? (
                    <>
                      <Loader2 size={16} className="animate-spin" />
                      Parsing Text...
                    </>
                  ) : (
                    <>
                      <Sparkles size={16} />
                      Parse Copied Text
                    </>
                  )}
                </button>
              </div>
            )}

            {/* Error Message Box */}
            {parseError && !isCorsRestricted && (
              <div style={{ 
                backgroundColor: 'var(--danger-light)', 
                color: 'var(--danger)', 
                border: '1px solid rgba(197, 48, 48, 0.2)', 
                padding: '16px', 
                borderRadius: '16px',
                fontSize: '13px',
                lineHeight: '1.4'
              }}>
                <strong>Error parsing:</strong> {parseError}
              </div>
            )}
          </div>
        ) : (
          /* MANUAL ENTRY OR REVIEW FORM */
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div style={{ backgroundColor: 'var(--card-bg)', padding: '20px', borderRadius: '20px', border: '1px solid var(--border)', boxShadow: 'var(--shadow-sm)' }}>
              <h3 style={{ fontSize: '15px', fontWeight: 800, color: 'var(--primary)', marginBottom: '14px' }}>Basic Info</h3>
              
              <div className="form-group">
                <label className="form-label">Recipe Title</label>
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="form-input"
                  placeholder="e.g. Grandma's Famous Lasagna"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="form-textarea"
                  placeholder="Tell something about this recipe..."
                />
              </div>

              <div className="form-group">
                <label className="form-label">Cover Image URL</label>
                <input
                  type="url"
                  value={image}
                  onChange={(e) => setImage(e.target.value)}
                  className="form-input"
                  placeholder="https://images.unsplash.com/photo-..."
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                <div className="form-group">
                  <label className="form-label">Category</label>
                  <select 
                    value={category} 
                    onChange={(e) => setCategory(e.target.value)}
                    className="form-select"
                  >
                    {CATEGORIES.map(cat => <option key={cat} value={cat}>{cat}</option>)}
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Difficulty</label>
                  <select 
                    value={difficulty} 
                    onChange={(e) => setDifficulty(e.target.value)}
                    className="form-select"
                  >
                    {DIFFICULTIES.map(diff => <option key={diff} value={diff}>{diff}</option>)}
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '10px' }}>
                <div className="form-group">
                  <label className="form-label">Servings</label>
                  <input
                    type="number"
                    min="1"
                    value={servings}
                    onChange={(e) => setServings(e.target.value)}
                    className="form-input"
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Prep Time (min)</label>
                  <input
                    type="number"
                    min="0"
                    value={prepTime}
                    onChange={(e) => setPrepTime(e.target.value)}
                    className="form-input"
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Cook Time (min)</label>
                  <input
                    type="number"
                    min="0"
                    value={cookTime}
                    onChange={(e) => setCookTime(e.target.value)}
                    className="form-input"
                  />
                </div>
              </div>
            </div>

            {/* Ingredients Section */}
            <div style={{ backgroundColor: 'var(--card-bg)', padding: '20px', borderRadius: '20px', border: '1px solid var(--border)', boxShadow: 'var(--shadow-sm)' }}>
              <h3 style={{ fontSize: '15px', fontWeight: 800, color: 'var(--primary)', marginBottom: '14px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                Ingredients
                <button
                  type="button"
                  onClick={handleAddIngredient}
                  className="btn btn-secondary"
                  style={{ padding: '4px 10px', borderRadius: '8px', fontSize: '12px' }}
                >
                  <Plus size={14} /> Add
                </button>
              </h3>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {ingredients.map((ingredient, idx) => (
                  <div key={idx} style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <input
                      type="text"
                      value={ingredient}
                      onChange={(e) => handleIngredientChange(idx, e.target.value)}
                      className="form-input"
                      placeholder={`Ingredient ${idx + 1}`}
                      style={{ flex: 1 }}
                    />
                    <button
                      type="button"
                      onClick={() => handleRemoveIngredient(idx)}
                      className="btn btn-ghost"
                      style={{ padding: '8px', color: 'var(--danger)', borderRadius: '10px' }}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                ))}
              </div>
            </div>

            {/* Steps Section */}
            <div style={{ backgroundColor: 'var(--card-bg)', padding: '20px', borderRadius: '20px', border: '1px solid var(--border)', boxShadow: 'var(--shadow-sm)' }}>
              <h3 style={{ fontSize: '15px', fontWeight: 800, color: 'var(--primary)', marginBottom: '14px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                Cooking Instructions
                <button
                  type="button"
                  onClick={handleAddStep}
                  className="btn btn-secondary"
                  style={{ padding: '4px 10px', borderRadius: '8px', fontSize: '12px' }}
                >
                  <Plus size={14} /> Add Step
                </button>
              </h3>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {steps.map((step, idx) => (
                  <div key={idx} style={{ display: 'flex', gap: '8px', alignItems: 'flex-start' }}>
                    <span style={{ fontSize: '13px', fontWeight: 700, color: 'var(--text-light)', paddingTop: '12px' }}>
                      {idx + 1}.
                    </span>
                    <textarea
                      value={step}
                      onChange={(e) => handleStepChange(idx, e.target.value)}
                      className="form-textarea"
                      placeholder={`Describe step ${idx + 1}...`}
                      style={{ flex: 1, minHeight: '60px' }}
                    />
                    <button
                      type="button"
                      onClick={() => handleRemoveStep(idx)}
                      className="btn btn-ghost"
                      style={{ padding: '8px', color: 'var(--danger)', borderRadius: '10px', marginTop: '4px' }}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                ))}
              </div>
            </div>

            {/* Save Recipe Button */}
            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', padding: '14px', borderRadius: '14px', fontSize: '16px', fontWeight: 700, marginBottom: '20px' }}
            >
              {isEditing ? 'Save Changes' : 'Create Recipe'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
