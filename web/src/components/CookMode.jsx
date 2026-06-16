import React, { useState, useEffect, useRef } from 'react';
import { ArrowLeft, Play, Pause, RotateCcw, ChevronLeft, ChevronRight, Check, Timer } from 'lucide-react';

export default function CookMode({ recipe, onExit }) {
  const [currentScreen, setCurrentScreen] = useState('checklist'); // 'checklist' or 'steps'
  const [checkedIngredients, setCheckedIngredients] = useState({});
  const [currentStepIdx, setCurrentStepIdx] = useState(0);
  
  // Timer States
  const [timerDuration, setTimerDuration] = useState(0); // in seconds
  const [timerSecondsLeft, setTimerSecondsLeft] = useState(0);
  const [isTimerRunning, setIsTimerRunning] = useState(false);
  const timerRef = useRef(null);

  // Parse timer from active step text
  const activeStepText = recipe.steps[currentStepIdx] || '';
  
  const detectTimerSeconds = (text) => {
    const hrRegex = /\b(\d+)\s*(?:-|to)?\s*(\d+)?\s*(?:hours|hour|hrs|hr)\b/i;
    const minRegex = /\b(\d+)\s*(?:-|to)?\s*(\d+)?\s*(?:minutes|minute|mins|min)\b/i;
    const secRegex = /\b(\d+)\s*(?:-|to)?\s*(\d+)?\s*(?:seconds|second|secs|sec)\b/i;

    let totalSeconds = 0;

    const hrMatch = text.match(hrRegex);
    if (hrMatch) {
      const hours = parseInt(hrMatch[2] || hrMatch[1]);
      totalSeconds += hours * 3600;
    }

    const minMatch = text.match(minRegex);
    if (minMatch) {
      const minutes = parseInt(minMatch[2] || minMatch[1]);
      totalSeconds += minutes * 60;
    }

    const secMatch = text.match(secRegex);
    if (secMatch) {
      const seconds = parseInt(secMatch[2] || secMatch[1]);
      totalSeconds += seconds;
    }

    return totalSeconds > 0 ? totalSeconds : null;
  };

  // Detect timer when active step changes
  useEffect(() => {
    if (currentScreen === 'steps') {
      const seconds = detectTimerSeconds(activeStepText);
      if (seconds) {
        setTimerDuration(seconds);
        setTimerSecondsLeft(seconds);
        setIsTimerRunning(false);
      } else {
        setTimerDuration(0);
        setTimerSecondsLeft(0);
        setIsTimerRunning(false);
      }
    }
  }, [currentStepIdx, currentScreen, activeStepText]);

  // Handle countdown interval
  useEffect(() => {
    if (isTimerRunning && timerSecondsLeft > 0) {
      timerRef.current = setInterval(() => {
        setTimerSecondsLeft((prev) => {
          if (prev <= 1) {
            setIsTimerRunning(false);
            clearInterval(timerRef.current);
            // Play notification sound
            try {
              const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
              const oscillator = audioCtx.createOscillator();
              const gainNode = audioCtx.createGain();
              oscillator.connect(gainNode);
              gainNode.connect(audioCtx.destination);
              oscillator.type = 'sine';
              oscillator.frequency.setValueAtTime(880, audioCtx.currentTime); // A5 note
              gainNode.gain.setValueAtTime(0.5, audioCtx.currentTime);
              oscillator.start();
              oscillator.stop(audioCtx.currentTime + 0.8);
            } catch (e) {
              console.warn("Could not play sound", e);
            }
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isTimerRunning, timerSecondsLeft]);

  const toggleIngredient = (idx) => {
    setCheckedIngredients((prev) => ({
      ...prev,
      [idx]: !prev[idx]
    }));
  };

  const handleNextStep = () => {
    if (currentStepIdx < recipe.steps.length - 1) {
      setCurrentStepIdx(currentStepIdx + 1);
    } else {
      // Completed last step, exit Cook Mode
      onExit();
    }
  };

  const handlePrevStep = () => {
    if (currentStepIdx > 0) {
      setCurrentStepIdx(currentStepIdx - 1);
    }
  };

  const formatTime = (secs) => {
    const h = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60);
    const s = secs % 60;
    return [
      h > 0 ? h : null,
      m.toString().padStart(2, '0'),
      s.toString().padStart(2, '0')
    ].filter(Boolean).join(':');
  };

  const percentComplete = currentScreen === 'checklist' 
    ? 0 
    : Math.round(((currentStepIdx + 1) / recipe.steps.length) * 100);

  return (
    <div className="cook-mode-view" style={{ 
      display: 'flex', 
      flexDirection: 'column', 
      height: '100%', 
      backgroundColor: 'var(--bg-mobile)', 
      color: 'var(--text-main)' 
    }}>
      {/* Header */}
      <div style={{ 
        padding: '16px 20px', 
        backgroundColor: 'var(--card-bg)', 
        borderBottom: '1px solid var(--border)',
        display: 'flex', 
        alignItems: 'center', 
        gap: '12px' 
      }}>
        <button 
          onClick={onExit}
          className="btn"
          style={{ padding: '8px', borderRadius: '50%', backgroundColor: 'transparent', color: 'var(--text-main)', border: 'none' }}
        >
          <ArrowLeft size={20} />
        </button>
        <div style={{ flex: 1 }}>
          <h3 style={{ fontSize: '15px', fontWeight: 700, color: 'var(--primary)', marginBottom: '2px' }}>
            COOKING MODE
          </h3>
          <p style={{ fontSize: '13px', color: 'var(--text-muted)', fontWeight: 600, display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '220px' }}>
            {recipe.title}
          </p>
        </div>
        <span style={{ fontSize: '13px', fontWeight: 700, color: 'var(--primary)' }}>
          {currentScreen === 'checklist' ? 'Prep' : `${currentStepIdx + 1}/${recipe.steps.length}`}
        </span>
      </div>

      {/* Progress Bar */}
      <div style={{ width: '100%', height: '4px', backgroundColor: 'var(--border)', position: 'relative' }}>
        <div style={{ 
          width: `${percentComplete}%`, 
          height: '100%', 
          backgroundColor: 'var(--primary)', 
          transition: 'width 0.3s ease-in-out' 
        }}></div>
      </div>

      {/* Main Content Area */}
      <div style={{ flex: 1, padding: '24px 20px', overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
        {currentScreen === 'checklist' ? (
          /* SCREEN 1: Ingredient Prep Checklist */
          <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <h2 style={{ fontSize: '20px', fontWeight: 800, marginBottom: '8px', color: 'var(--text-main)' }}>
              Gather Ingredients
            </h2>
            <p style={{ fontSize: '14px', color: 'var(--text-muted)', marginBottom: '20px' }}>
              Check off ingredients as you measure them out to ensure a smooth cooking flow.
            </p>

            <ul style={{ listStyleType: 'none', display: 'flex', flexDirection: 'column', gap: '10px', flex: 1 }}>
              {recipe.ingredients.map((ingredient, idx) => (
                <li 
                  key={idx}
                  onClick={() => toggleIngredient(idx)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    padding: '14px 16px',
                    borderRadius: '16px',
                    backgroundColor: checkedIngredients[idx] ? 'var(--primary-light)' : 'var(--card-bg)',
                    border: checkedIngredients[idx] ? '1.5px solid var(--primary)' : '1px solid var(--border)',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    textDecoration: checkedIngredients[idx] ? 'line-through' : 'none',
                    color: checkedIngredients[idx] ? 'var(--text-muted)' : 'var(--text-main)',
                    fontSize: '14px',
                    fontWeight: 600
                  }}
                >
                  <div style={{
                    width: '20px',
                    height: '20px',
                    borderRadius: '6px',
                    border: checkedIngredients[idx] ? 'none' : '2px solid var(--text-light)',
                    backgroundColor: checkedIngredients[idx] ? 'var(--primary)' : 'transparent',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'white',
                    flexShrink: 0
                  }}>
                    {checkedIngredients[idx] && <Check size={14} strokeWidth={3} />}
                  </div>
                  <span>{ingredient}</span>
                </li>
              ))}
            </ul>

            <button 
              onClick={() => setCurrentScreen('steps')}
              className="btn btn-primary"
              style={{ width: '100%', padding: '14px', borderRadius: '14px', fontSize: '16px', fontWeight: 700, marginTop: '24px' }}
            >
              Start Cooking Steps
            </button>
          </div>
        ) : (
          /* SCREEN 2: Step-by-Step Cooking */
          <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between', height: '100%', flex: 1 }}>
            
            {/* Step Card */}
            <div style={{ 
              backgroundColor: 'var(--card-bg)', 
              borderRadius: '24px', 
              padding: '24px', 
              boxShadow: 'var(--shadow-lg)', 
              border: '1px solid var(--border)',
              display: 'flex',
              flexDirection: 'column',
              flex: 1,
              justifyContent: 'center',
              minHeight: '260px'
            }}>
              <span style={{ 
                fontSize: '13px', 
                fontWeight: 700, 
                color: 'var(--primary)', 
                textTransform: 'uppercase', 
                letterSpacing: '1px',
                marginBottom: '12px',
                display: 'block'
              }}>
                Step {currentStepIdx + 1}
              </span>
              <p style={{ 
                fontSize: '18px', 
                fontWeight: 600, 
                color: 'var(--text-main)', 
                lineHeight: '1.6', 
                textAlign: 'left',
                margin: 0
              }}>
                {activeStepText}
              </p>
            </div>

            {/* Step-Specific Timer Component */}
            {timerDuration > 0 && (
              <div style={{ 
                backgroundColor: 'var(--card-bg)', 
                borderRadius: '20px', 
                padding: '16px 20px', 
                marginTop: '20px',
                border: '1px solid var(--border)',
                boxShadow: 'var(--shadow-md)',
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'space-between' 
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <div style={{ 
                    width: '40px', 
                    height: '40px', 
                    borderRadius: '50%', 
                    backgroundColor: isTimerRunning ? 'var(--primary-light)' : 'var(--border)', 
                    display: 'flex', 
                    alignItems: 'center', 
                    justifyContent: 'center', 
                    color: isTimerRunning ? 'var(--primary)' : 'var(--text-muted)',
                    animation: isTimerRunning ? 'pulse 2s infinite' : 'none'
                  }}>
                    <Timer size={20} />
                  </div>
                  <div>
                    <span style={{ fontSize: '11px', fontWeight: 700, color: 'var(--text-light)', uppercase: 'true', display: 'block' }}>
                      STEP TIMER
                    </span>
                    <span style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-main)' }}>
                      {formatTime(timerSecondsLeft)}
                    </span>
                  </div>
                </div>
                
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button 
                    onClick={() => {
                      setTimerSecondsLeft(timerDuration);
                      setIsTimerRunning(false);
                    }}
                    className="btn btn-ghost"
                    style={{ padding: '8px', borderRadius: '50%', border: '1px solid var(--border)' }}
                    title="Reset Timer"
                  >
                    <RotateCcw size={16} />
                  </button>
                  <button 
                    onClick={() => setIsTimerRunning(!isTimerRunning)}
                    className="btn btn-primary"
                    style={{ 
                      padding: '8px 16px', 
                      borderRadius: '12px', 
                      backgroundColor: isTimerRunning ? 'var(--text-main)' : 'var(--primary)',
                      color: 'white'
                    }}
                  >
                    {isTimerRunning ? <Pause size={16} /> : <Play size={16} fill="currentColor" />}
                    <span style={{ marginLeft: '6px', fontSize: '13px' }}>{isTimerRunning ? 'Pause' : 'Start'}</span>
                  </button>
                </div>
              </div>
            )}

            {/* Navigation Buttons */}
            <div style={{ display: 'flex', gap: '16px', marginTop: '24px' }}>
              <button
                onClick={handlePrevStep}
                disabled={currentStepIdx === 0}
                className="btn btn-secondary"
                style={{ 
                  flex: 1, 
                  padding: '14px', 
                  borderRadius: '14px',
                  opacity: currentStepIdx === 0 ? 0.4 : 1,
                  cursor: currentStepIdx === 0 ? 'not-allowed' : 'pointer'
                }}
              >
                <ChevronLeft size={20} />
                Back
              </button>
              
              <button
                onClick={handleNextStep}
                className="btn btn-primary"
                style={{ 
                  flex: 2, 
                  padding: '14px', 
                  borderRadius: '14px',
                  fontSize: '15px',
                  fontWeight: 700
                }}
              >
                {currentStepIdx === recipe.steps.length - 1 ? 'Finish!' : 'Next'}
                <ChevronRight size={20} />
              </button>
            </div>

          </div>
        )}
      </div>
    </div>
  );
}
