import { Search, Plus } from 'lucide-react';
import { useEffect, useRef } from 'react';

interface AutocompleteSuggestionsProps {
  suggestions: string[];
  onSelect: (suggestion: string) => void;
  selectedIndex?: number;
}

interface AutocompleteSuggestionsProps {
  suggestions: string[];
  onSelect: (suggestion: string) => void;
  onFill?: (suggestion: string) => void; // Nouvelle prop pour juste remplir
  selectedIndex?: number;
}

const AutocompleteSuggestions = ({ suggestions, onSelect, onFill, selectedIndex = -1 }: AutocompleteSuggestionsProps) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const selectedItemRef = useRef<HTMLDivElement>(null);

  // Effet pour faire défiler automatiquement vers l'élément sélectionné
  useEffect(() => {
    if (selectedIndex >= 0 && selectedItemRef.current && containerRef.current) {
      const container = containerRef.current;
      const selectedItem = selectedItemRef.current;
      
      const containerHeight = container.clientHeight;
      const containerScrollTop = container.scrollTop;
      const itemTop = selectedItem.offsetTop;
      const itemHeight = selectedItem.offsetHeight;
      
      // Vérifier si l'élément est visible
      const isAboveView = itemTop < containerScrollTop;
      const isBelowView = itemTop + itemHeight > containerScrollTop + containerHeight;
      
      if (isAboveView) {
        // Défiler vers le haut pour montrer l'élément
        container.scrollTo({
          top: itemTop,
          behavior: 'smooth'
        });
      } else if (isBelowView) {
        // Défiler vers le bas pour montrer l'élément
        container.scrollTo({
          top: itemTop + itemHeight - containerHeight,
          behavior: 'smooth'
        });
      }
    }
  }, [selectedIndex]);

  return (
    <div 
      ref={containerRef}
      className="absolute z-50 w-full bg-white border border-gray-200 rounded-xl shadow-2xl mt-2 max-h-60 overflow-y-auto mb-200"
    >
      {suggestions.map((suggestion, index) => (
        <div
          key={index}
          ref={index === selectedIndex ? selectedItemRef : null}
          onClick={() => onSelect(suggestion)}
          className={`p-3 cursor-pointer transition-all duration-200 border-b border-gray-100 last:border-b-0 flex items-center justify-between group ${
            index === selectedIndex
              ? 'bg-gradient-to-r from-teal-50 to-cyan-50 border-teal-200'
              : 'hover:bg-gradient-to-r hover:from-teal-50 hover:to-cyan-50'
          }`}
        >
          <div className="flex items-center gap-3 flex-1">
            <Search size={16} className="text-gray-400" />
            <span className="text-gray-700">{suggestion}</span>
          </div>
          <div 
            onClick={(e) => {
              e.stopPropagation(); // Empêche le click de remonter
              onFill?.(suggestion); // Utilise onFill au lieu de onSelect
            }}
            className={`p-2 rounded-full transition-all duration-200 ${
              index === selectedIndex
                ? 'bg-teal-500 text-white'
                : 'bg-gray-100 text-gray-400 group-hover:bg-teal-100 group-hover:text-teal-600'
            }`}
            title="Remplir avec cette suggestion"
          >
            <Plus size={18} />
          </div>
        </div>
      ))}
    </div>
  );
};

export default AutocompleteSuggestions;