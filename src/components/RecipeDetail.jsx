import React, { useState } from 'react';
import { ArrowLeft, Clock, Users, Flame, BookOpen, Trash2, Edit2, Play } from 'lucide-react';

export default function RecipeDetail({ recipe, onBack, onStartCooking, onEditRecipe, onDeleteRecipe }) {
  const [activeTab, setActiveTab] = useState('ingredients'); // 'ingredients' or 'steps'
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  if (!recipe) return null;

  const totalTime = recipe.prepTime + recipe.cookTime;

  return (
    <div className="recipe-detail-view" style={{ display: 'flex', flexDirection: 'column', height: '100%', backgroundColor: 'var(--bg-mobile)' }}>
      {/* Recipe Header (Image & Back Button) */}
      <div style={{ width: '100%', height: '220px', position: 'relative', overflow: 'hidden' }}>
        <img 
          src={recipe.image || 'https://images.unsplash.com/photo-1495521821757-a1efb6729352?auto=format&fit=crop&w=800&q=80'} 
          alt={recipe.title} 
          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
        />
        {/* Soft shadow overlay for legibility */}
        <div style={{
          position: 'absolute',
          inset: 0,
          background: 'linear-gradient(to bottom, rgba(0,0,0,0.4) 0%, rgba(0,0,0,0) 40%, rgba(0,0,0,0.6) 100%)'
        }}></div>

        {/* Back Button */}
        <button 
          onClick={onBack}
          className="btn"
          style={{
            position: 'absolute',
            top: '16px',
            left: '16px',
            padding: '10px',
            borderRadius: '50%',
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            backdropFilter: 'blur(4px)',
            color: 'white',
            border: 'none',
            zIndex: 5
          }}
        >
          <ArrowLeft size={18} />
        </button>

        {/* Action Buttons (Edit / Delete) */}
        <div style={{ position: 'absolute', top: '16px', right: '16px', display: 'flex', gap: '8px', zIndex: 5 }}>
          <button 
            onClick={onEditRecipe}
            className="btn"
            style={{
              padding: '10px',
              borderRadius: '50%',
              backgroundColor: 'rgba(0, 0, 0, 0.5)',
              backdropFilter: 'blur(4px)',
              color: 'white',
              border: 'none',
            }}
          >
            <Edit2 size={16} />
          </button>
          <button 
            onClick={() => setShowDeleteConfirm(true)}
            className="btn"
            style={{
              padding: '10px',
              borderRadius: '50%',
              backgroundColor: 'rgba(197, 48, 48, 0.8)',
              backdropFilter: 'blur(4px)',
              color: 'white',
              border: 'none',
            }}
          >
            <Trash2 size={16} />
          </button>
        </div>

        {/* Category & Title Overlays */}
        <div style={{ position: 'absolute', bottom: '16px', left: '20px', right: '20px' }}>
          <span style={{
            backgroundColor: 'var(--primary)',
            color: 'white',
            padding: '2px 8px',
            borderRadius: '6px',
            fontSize: '11px',
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
            display: 'inline-block',
            marginBottom: '6px'
          }}>
            {recipe.category}
          </span>
          <h2 style={{ color: 'white', fontSize: '24px', fontWeight: 800, margin: 0, textShadow: '0 2px 4px rgba(0,0,0,0.5)', lineHeight: '1.2' }}>
            {recipe.title}
          </h2>
        </div>
      </div>

      {/* Main Recipe Info Block */}
      <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
        {/* Quick Info Grid */}
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(3, 1fr)', 
          gap: '12px', 
          padding: '16px 20px', 
          backgroundColor: 'var(--card-bg)',
          borderBottom: '1px solid var(--border)',
          textAlign: 'center'
        }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px' }}>
            <Clock size={18} style={{ color: 'var(--primary)' }} />
            <span style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-light)', uppercase: 'true' }}>COOK TIME</span>
            <span style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-main)' }}>{totalTime} mins</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px', borderLeft: '1px solid var(--border)', borderRight: '1px solid var(--border)' }}>
            <Users size={18} style={{ color: 'var(--primary)' }} />
            <span style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-light)', uppercase: 'true' }}>SERVINGS</span>
            <span style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-main)' }}>{recipe.servings} serving{recipe.servings > 1 ? 's' : ''}</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px' }}>
            <Flame size={18} style={{ color: 'var(--primary)' }} />
            <span style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-light)', uppercase: 'true' }}>DIFFICULTY</span>
            <span style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-main)' }}>{recipe.difficulty}</span>
          </div>
        </div>

        {/* Description */}
        {recipe.description && (
          <div style={{ padding: '16px 20px', backgroundColor: 'var(--card-bg)', borderBottom: '1px solid var(--border)' }}>
            <p style={{ fontSize: '14px', color: 'var(--text-muted)', lineHeight: '1.5', fontStyle: 'italic' }}>
              "{recipe.description}"
            </p>
          </div>
        )}

        {/* Tabs Control */}
        <div style={{ 
          display: 'flex', 
          borderBottom: '1px solid var(--border)', 
          backgroundColor: 'var(--card-bg)',
          position: 'sticky',
          top: 0,
          zIndex: 2
        }}>
          <button 
            onClick={() => setActiveTab('ingredients')}
            style={{ 
              flex: 1, 
              padding: '14px 0', 
              fontSize: '14px', 
              fontWeight: 700, 
              color: activeTab === 'ingredients' ? 'var(--primary)' : 'var(--text-muted)',
              border: 'none',
              backgroundColor: 'transparent',
              borderBottom: activeTab === 'ingredients' ? '3px solid var(--primary)' : '3px solid transparent',
              cursor: 'pointer'
            }}
          >
            Ingredients ({recipe.ingredients.length})
          </button>
          <button 
            onClick={() => setActiveTab('steps')}
            style={{ 
              flex: 1, 
              padding: '14px 0', 
              fontSize: '14px', 
              fontWeight: 700, 
              color: activeTab === 'steps' ? 'var(--primary)' : 'var(--text-muted)',
              border: 'none',
              backgroundColor: 'transparent',
              borderBottom: activeTab === 'steps' ? '3px solid var(--primary)' : '3px solid transparent',
              cursor: 'pointer'
            }}
          >
            Instructions ({recipe.steps.length})
          </button>
        </div>

        {/* Tab Contents */}
        <div style={{ padding: '20px', flex: 1 }}>
          {activeTab === 'ingredients' ? (
            /* Ingredients List */
            <ul style={{ listStyleType: 'none', display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {recipe.ingredients.map((ingredient, idx) => (
                <li 
                  key={idx}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    padding: '12px 16px',
                    borderRadius: '12px',
                    backgroundColor: 'var(--card-bg)',
                    border: '1px solid var(--border)',
                    fontSize: '14px',
                    fontWeight: 500,
                    color: 'var(--text-main)'
                  }}
                >
                  <span style={{
                    width: '6px',
                    height: '6px',
                    borderRadius: '50%',
                    backgroundColor: 'var(--primary)',
                    flexShrink: 0
                  }}></span>
                  {ingredient}
                </li>
              ))}
            </ul>
          ) : (
            /* Steps List */
            <ol style={{ listStyleType: 'none', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {recipe.steps.map((step, idx) => (
                <li 
                  key={idx}
                  style={{
                    display: 'flex',
                    gap: '14px',
                    padding: '16px',
                    borderRadius: '16px',
                    backgroundColor: 'var(--card-bg)',
                    border: '1px solid var(--border)'
                  }}
                >
                  {/* Step Number Badge */}
                  <span style={{
                    width: '28px',
                    height: '28px',
                    borderRadius: '50%',
                    backgroundColor: 'var(--primary-light)',
                    color: 'var(--primary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifycontent: 'center',
                    justifyContent: 'center',
                    fontSize: '14px',
                    fontWeight: 700,
                    flexShrink: 0
                  }}>
                    {idx + 1}
                  </span>
                  <p style={{ fontSize: '14px', color: 'var(--text-main)', lineHeight: '1.5', paddingTop: '2px' }}>
                    {step}
                  </p>
                </li>
              ))}
            </ol>
          )}
        </div>
      </div>

      {/* Start Cooking Action Bar */}
      <div style={{ 
        padding: '16px 20px', 
        backgroundColor: 'var(--card-bg)', 
        borderTop: '1px solid var(--border)',
        boxShadow: '0 -4px 10px rgba(0,0,0,0.02)'
      }}>
        <button 
          onClick={onStartCooking}
          className="btn btn-primary"
          style={{ width: '100%', padding: '14px', borderRadius: '14px', fontSize: '16px', fontWeight: 700, display: 'flex', gap: '8px', justifyContent: 'center' }}
        >
          <Play size={18} fill="currentColor" />
          Start Cooking (Cook Mode)
        </button>
      </div>

      {/* Delete Confirmation Dialog */}
      {showDeleteConfirm && (
        <div style={{
          position: 'absolute',
          inset: 0,
          backgroundColor: 'rgba(0,0,0,0.5)',
          backdropFilter: 'blur(4px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '20px',
          zIndex: 10
        }}>
          <div style={{
            backgroundColor: 'var(--card-bg)',
            borderRadius: '24px',
            padding: '24px',
            width: '100%',
            maxWidth: '320px',
            textAlign: 'center',
            boxShadow: 'var(--shadow-lg)'
          }}>
            <h3 style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-main)', marginBottom: '8px' }}>Delete Recipe?</h3>
            <p style={{ fontSize: '14px', color: 'var(--text-muted)', marginBottom: '20px', lineHeight: '1.4' }}>
              Are you sure you want to delete this recipe? This action cannot be undone.
            </p>
            <div style={{ display: 'flex', gap: '12px' }}>
              <button 
                onClick={() => setShowDeleteConfirm(false)}
                className="btn btn-ghost"
                style={{ flex: 1, border: '1px solid var(--border)' }}
              >
                Cancel
              </button>
              <button 
                onClick={() => {
                  setShowDeleteConfirm(false);
                  onDeleteRecipe(recipe.id);
                }}
                className="btn btn-primary"
                style={{ flex: 1, backgroundColor: 'var(--danger)', color: 'white' }}
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
