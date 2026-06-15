import React, { useState } from 'react';
import { Search, Clock, Users, ChefHat, Plus, Settings } from 'lucide-react';

const CATEGORIES = ['All', 'Breakfast', 'Lunch', 'Dinner', 'Dessert', 'Snack'];

export default function Dashboard({ recipes, onSelectRecipe, onAddRecipeClick, onOpenSettings }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');

  // Filter recipes based on search query and category
  const filteredRecipes = recipes.filter(recipe => {
    const matchesSearch = recipe.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          recipe.ingredients.some(ing => ing.toLowerCase().includes(searchQuery.toLowerCase())) ||
                          (recipe.description && recipe.description.toLowerCase().includes(searchQuery.toLowerCase()));
    
    const matchesCategory = selectedCategory === 'All' || recipe.category === selectedCategory;
    
    return matchesSearch && matchesCategory;
  });

  return (
    <div className="dashboard-view" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Search and Settings Header */}
      <div className="search-bar-container" style={{ padding: '16px 20px', backgroundColor: 'var(--card-bg)', borderBottom: '1px solid var(--border)' }}>
        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <Search 
              size={18} 
              style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-light)' }} 
            />
            <input
              type="text"
              placeholder="Search recipes or ingredients..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="form-input"
              style={{ paddingLeft: '38px', borderRadius: '14px' }}
            />
          </div>
          <button 
            onClick={onOpenSettings}
            className="btn btn-ghost" 
            style={{ padding: '10px', borderRadius: '14px', border: '1px solid var(--border)' }}
            title="Settings"
          >
            <Settings size={18} />
          </button>
        </div>
      </div>

      {/* Categories Horizontal Scroll */}
      <div className="categories-container" style={{ 
        padding: '12px 20px', 
        overflowX: 'auto', 
        whiteSpace: 'nowrap', 
        display: 'flex', 
        gap: '8px',
        backgroundColor: 'var(--card-bg)',
        borderBottom: '1px solid var(--border)',
      }}>
        {CATEGORIES.map(category => (
          <button
            key={category}
            onClick={() => setSelectedCategory(category)}
            className={`btn ${selectedCategory === category ? 'btn-primary' : 'btn-ghost'}`}
            style={{
              padding: '6px 14px',
              borderRadius: '20px',
              fontSize: '13px',
              fontWeight: 600,
              backgroundColor: selectedCategory === category ? 'var(--primary)' : 'var(--primary-light)',
              color: selectedCategory === category ? 'var(--text-on-primary)' : 'var(--primary)',
              border: selectedCategory === category ? 'none' : '1px solid rgba(224, 106, 59, 0.1)',
            }}
          >
            {category}
          </button>
        ))}
      </div>

      {/* Recipes List/Grid */}
      <div className="recipes-list" style={{ flex: 1, padding: '20px', overflowY: 'auto' }}>
        {filteredRecipes.length === 0 ? (
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--text-muted)', textAlign: 'center', padding: '40px 0' }}>
            <ChefHat size={48} style={{ color: 'var(--text-light)', marginBottom: '16px' }} />
            <h3 style={{ fontSize: '18px', fontWeight: 600, color: 'var(--text-main)', marginBottom: '6px' }}>No recipes found</h3>
            <p style={{ fontSize: '14px' }}>Try adjusting your search or category filter, or add a new recipe!</p>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '20px' }}>
            {filteredRecipes.map(recipe => (
              <div 
                key={recipe.id}
                onClick={() => onSelectRecipe(recipe.id)}
                style={{
                  backgroundColor: 'var(--card-bg)',
                  borderRadius: '20px',
                  overflow: 'hidden',
                  boxShadow: 'var(--shadow-md)',
                  cursor: 'pointer',
                  border: '1px solid var(--border)',
                  transition: 'transform 0.2s, box-shadow 0.2s',
                  position: 'relative'
                }}
                className="recipe-card"
              >
                {/* Recipe Image */}
                <div style={{ width: '100%', height: '150px', position: 'relative', overflow: 'hidden', backgroundColor: 'var(--primary-light)' }}>
                  <img 
                    src={recipe.image || 'https://images.unsplash.com/photo-1495521821757-a1efb6729352?auto=format&fit=crop&w=800&q=80'} 
                    alt={recipe.title}
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                  {/* Category Tag */}
                  <span style={{
                    position: 'absolute',
                    top: '12px',
                    left: '12px',
                    backgroundColor: 'rgba(0, 0, 0, 0.6)',
                    backdropFilter: 'blur(4px)',
                    color: 'white',
                    padding: '4px 10px',
                    borderRadius: '20px',
                    fontSize: '11px',
                    fontWeight: 700,
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px'
                  }}>
                    {recipe.category}
                  </span>
                </div>

                {/* Recipe Details */}
                <div style={{ padding: '16px' }}>
                  <h3 style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-main)', marginBottom: '6px', lineHeight: '1.2' }}>
                    {recipe.title}
                  </h3>
                  {recipe.description && (
                    <p style={{ 
                      fontSize: '13px', 
                      color: 'var(--text-muted)', 
                      marginBottom: '14px', 
                      display: '-webkit-box', 
                      WebkitLineClamp: 2, 
                      WebkitBoxOrient: 'vertical', 
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      lineHeight: '1.4'
                    }}>
                      {recipe.description}
                    </p>
                  )}

                  {/* Metadata Row */}
                  <div style={{ display: 'flex', gap: '16px', alignItems: 'center', borderTop: '1px solid var(--border)', paddingTop: '12px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '5px', fontSize: '12px', color: 'var(--text-muted)' }}>
                      <Clock size={14} style={{ color: 'var(--primary)' }} />
                      <span>{recipe.prepTime + recipe.cookTime} mins</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '5px', fontSize: '12px', color: 'var(--text-muted)' }}>
                      <Users size={14} style={{ color: 'var(--primary)' }} />
                      <span>{recipe.servings} serving{recipe.servings > 1 ? 's' : ''}</span>
                    </div>
                    <div style={{ 
                      marginLeft: 'auto',
                      fontSize: '11px', 
                      fontWeight: 700, 
                      padding: '2px 8px', 
                      borderRadius: '6px',
                      backgroundColor: recipe.difficulty === 'Easy' ? 'var(--success-light)' : recipe.difficulty === 'Medium' ? 'rgba(224, 106, 59, 0.1)' : 'var(--danger-light)',
                      color: recipe.difficulty === 'Easy' ? 'var(--success)' : recipe.difficulty === 'Medium' ? 'var(--primary)' : 'var(--danger)'
                    }}>
                      {recipe.difficulty}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Floating Action Button */}
      <button 
        onClick={onAddRecipeClick}
        className="fab" 
        aria-label="Add Recipe"
      >
        <Plus size={24} />
      </button>
    </div>
  );
}
