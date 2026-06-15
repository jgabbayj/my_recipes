import React, { useState, useEffect, useRef } from 'react';
import { ChefHat, BookOpen, Settings, Key, HelpCircle, Save, X } from 'lucide-react';
import { recipeStore } from './services/recipeStore';
import Dashboard from './components/Dashboard';
import RecipeDetail from './components/RecipeDetail';
import CookMode from './components/CookMode';
import RecipeForm from './components/RecipeForm';

export default function App() {
  const [recipes, setRecipes] = useState([]);
  const [view, setView] = useState('dashboard'); // 'dashboard', 'detail', 'form', 'cook'
  const [selectedRecipeId, setSelectedRecipeId] = useState(null);
  const [editingRecipe, setEditingRecipe] = useState(null);
  const [apiKey, setApiKey] = useState('');
  
  // Refs
  const settingsDialogRef = useRef(null);
  const [inputApiKey, setInputApiKey] = useState('');

  // Initial Load
  useEffect(() => {
    // Load recipes
    const allRecipes = recipeStore.getAll();
    setRecipes(allRecipes);

    // Load API Key
    const savedKey = localStorage.getItem('gemini_api_key');
    if (savedKey) {
      setApiKey(savedKey);
      setInputApiKey(savedKey);
    }
  }, []);

  // Set up dialog fallback listener for light-dismiss (safari support)
  useEffect(() => {
    const dialog = settingsDialogRef.current;
    if (dialog && !('closedBy' in HTMLDialogElement.prototype)) {
      const handleLightDismiss = (event) => {
        if (event.target !== dialog) return;
        const rect = dialog.getBoundingClientRect();
        const isDialogContent = (
          rect.top <= event.clientY &&
          event.clientY <= rect.top + rect.height &&
          rect.left <= event.clientX &&
          event.clientX <= rect.left + rect.width
        );
        if (!isDialogContent) {
          dialog.close();
        }
      };
      dialog.addEventListener('click', handleLightDismiss);
      return () => dialog.removeEventListener('click', handleLightDismiss);
    }
  }, []);

  const handleSelectRecipe = (id) => {
    setSelectedRecipeId(id);
    setView('detail');
  };

  const handleSaveRecipe = (recipeData) => {
    if (recipeData.id) {
      recipeStore.update(recipeData.id, recipeData);
    } else {
      recipeStore.add(recipeData);
    }
    // Refresh recipes list
    setRecipes(recipeStore.getAll());
    
    // Go to detail if editing/added, or back to dashboard
    if (recipeData.id) {
      setView('detail');
    } else {
      setView('dashboard');
    }
  };

  const handleDeleteRecipe = (id) => {
    recipeStore.delete(id);
    setRecipes(recipeStore.getAll());
    setView('dashboard');
    setSelectedRecipeId(null);
  };

  const handleOpenSettings = () => {
    if (settingsDialogRef.current) {
      settingsDialogRef.current.showModal();
    }
  };

  const handleCloseSettings = () => {
    if (settingsDialogRef.current) {
      settingsDialogRef.current.close();
    }
  };

  const handleSaveApiKey = (e) => {
    e.preventDefault();
    localStorage.setItem('gemini_api_key', inputApiKey);
    setApiKey(inputApiKey);
    handleCloseSettings();
  };

  const activeRecipe = recipes.find(r => r.id === selectedRecipeId);

  return (
    <div className="phone-container">
      {/* App Header (Shows on Dashboard, Detail, Form but NOT Cook Mode which is full-screen distraction-free) */}
      {view !== 'cook' && (
        <header className="app-header">
          <div 
            className="app-title" 
            onClick={() => setView('dashboard')}
            style={{ cursor: 'pointer' }}
          >
            <ChefHat size={24} strokeWidth={2.5} />
            <span>MyReceipies</span>
          </div>
          <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-light)', backgroundColor: 'var(--primary-light)', padding: '4px 10px', borderRadius: '12px', color: 'var(--primary)' }}>
            {recipes.length} Recipe{recipes.length !== 1 ? 's' : ''}
          </div>
        </header>
      )}

      {/* Main Content Area */}
      <main className="app-content">
        {view === 'dashboard' && (
          <Dashboard
            recipes={recipes}
            onSelectRecipe={handleSelectRecipe}
            onAddRecipeClick={() => {
              setEditingRecipe(null);
              setView('form');
            }}
            onOpenSettings={handleOpenSettings}
          />
        )}

        {view === 'detail' && (
          <RecipeDetail
            recipe={activeRecipe}
            onBack={() => setView('dashboard')}
            onStartCooking={() => setView('cook')}
            onEditRecipe={() => {
              setEditingRecipe(activeRecipe);
              setView('form');
            }}
            onDeleteRecipe={handleDeleteRecipe}
          />
        )}

        {view === 'cook' && (
          <CookMode
            recipe={activeRecipe}
            onExit={() => setView('detail')}
          />
        )}

        {view === 'form' && (
          <RecipeForm
            recipe={editingRecipe}
            apiKey={apiKey}
            onSave={handleSaveRecipe}
            onBack={() => setView(editingRecipe ? 'detail' : 'dashboard')}
          />
        )}
      </main>

      {/* Settings Dialog (Declarative light-dismiss with closedby="any") */}
      <dialog 
        ref={settingsDialogRef} 
        closedby="any" 
        aria-labelledby="dialogTitle"
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 id="dialogTitle" style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Settings size={20} style={{ color: 'var(--primary)' }} />
            Settings
          </h3>
          <button 
            onClick={handleCloseSettings} 
            className="btn btn-ghost" 
            style={{ padding: '4px', borderRadius: '50%' }}
          >
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSaveApiKey} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div className="form-group">
            <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
              <Key size={14} /> Gemini API Key
            </label>
            <input
              type="password"
              className="form-input"
              placeholder="Paste your Gemini API Key here..."
              value={inputApiKey}
              onChange={(e) => setInputApiKey(e.target.value)}
              style={{ borderRadius: '12px' }}
            />
          </div>

          <div style={{ 
            backgroundColor: 'var(--primary-light)', 
            border: '1px solid rgba(224, 106, 59, 0.15)', 
            padding: '12px', 
            borderRadius: '12px',
            fontSize: '12px',
            color: 'var(--text-muted)',
            lineHeight: '1.4'
          }}>
            <h4 style={{ fontWeight: 700, color: 'var(--primary)', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}>
              <HelpCircle size={12} /> About AI Parsing
            </h4>
            To parse recipes directly from cooking websites, we use Google's Gemini 1.5 Flash model. 
            If you don't have an API key, you can get one for free at Google AI Studio. 
            When no key is configured, a mock recipe simulator will be loaded for demonstration.
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', borderRadius: '12px', padding: '12px', gap: '8px' }}
          >
            <Save size={16} />
            Save Settings
          </button>
        </form>
      </dialog>
    </div>
  );
}
