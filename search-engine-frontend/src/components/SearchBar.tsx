import { useState, useEffect, useRef } from 'react';
import { search, getAutocompleteSuggestions } from '../services/api';
import type { SearchRequest } from '../types/SearchRequest';
import type { AutocompleteRequest } from '../types/AutocompleteRequest';
import type { SearchContext } from '../types/SearchContext';
import AutocompleteSuggestions from './AutocompleteSuggestions';
import { Search, X, ArrowRight } from 'lucide-react';

interface SearchBarProps {
  setResults: (results: any[]) => void;
  setTotalResults: (total: number) => void;
  setSearchContext: (context: SearchContext | null) => void;
  currentFilter: string;
  currentPage: number;
}

const FILTER_TO_DOCUMENT_TYPE: Record<string, string | undefined> = {
  'ALL': undefined,
  'PDF': 'pdf',
  'IMAGE': 'image',
  'VIDEO': 'video',
  'WEB_PAGE': 'web_page'
};

const SearchBar = ({ setResults, setTotalResults, setSearchContext, currentFilter, currentPage }: SearchBarProps) => {
  const [query, setQuery] = useState('');
  const [displayQuery, setDisplayQuery] = useState('');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [completion, setCompletion] = useState<string | null>(null);
  const [isExtension, setIsExtension] = useState<boolean>(false);
  const [selectedSuggestionIndex, setSelectedSuggestionIndex] = useState(-1);
  const [showSuggestions, setShowSuggestions] = useState(false);

  const inputRef = useRef<HTMLInputElement>(null);
  const completionRef = useRef<HTMLSpanElement>(null);

  const handleSuggestionFill = (suggestion: string) => {
    setQuery(suggestion);
    setDisplayQuery(suggestion);
    setShowSuggestions(false);
    setCompletion(null);
    setSelectedSuggestionIndex(-1);
    if (inputRef.current) {
      inputRef.current.focus();
    }
  };

  const handleSearch = async () => {
    if (!query) return;

    setIsLoading(true);
    setShowSuggestions(false);

    const documentType = FILTER_TO_DOCUMENT_TYPE[currentFilter];

    const request: SearchRequest = {
      query,
      page: currentPage,
      size: 10,
      ...(documentType && { documentType: documentType as any })
    };

    try {
      console.log(request);
      const response = await search(request);
      console.log(response);
      setResults(response.results);
      setTotalResults(response.totalResults);
      setSearchContext(response.context || null);
    } catch (error) {
      console.error('Search failed:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleAutocomplete = async () => {
    if (query.length < 2) {
      setSuggestions([]);
      setCompletion(null);
      setShowSuggestions(false);
      return;
    }
    const request: AutocompleteRequest = {
      prefix: query,
      language: navigator.language,
      maxSuggestions: 5,
    };
    try {
      const response = await getAutocompleteSuggestions(request);
      console.log(response);
      setSuggestions(response.suggestions);
      setCompletion(response.completion);
      setIsExtension(response.isExtension);
      setShowSuggestions(true);
      setSelectedSuggestionIndex(-1);

      setDisplayQuery(query);
    } catch (error) {
      console.error('Autocomplete failed:', error);
      setCompletion(null);
      setDisplayQuery(query);
    }
  };

  const clearSearch = () => {
    setQuery('');
    setDisplayQuery('');
    setCompletion(null);
    setSuggestions([]);
    setShowSuggestions(false);
    setSelectedSuggestionIndex(-1);
    if (inputRef.current) {
      inputRef.current.focus();
    }
  };

  const acceptCompletion = () => {
    if (completion) {
      // Ajouter un espace pour les non-extensions, rien pour les extensions
      const newQuery = isExtension ? query + completion : query + '' + completion;
      setQuery(newQuery);
      setDisplayQuery(newQuery);
      setCompletion(null);
      setShowSuggestions(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      if (selectedSuggestionIndex >= 0) {
        setQuery(suggestions[selectedSuggestionIndex]);
        setDisplayQuery(suggestions[selectedSuggestionIndex]);
        setShowSuggestions(false);
        setSelectedSuggestionIndex(-1);
        handleSearch();
      } else {
        handleSearch();
      }
    } else if (e.key === 'ArrowRight' && completion) {
      e.preventDefault();
      acceptCompletion();
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (showSuggestions && suggestions.length > 0) {
        setSelectedSuggestionIndex(prev =>
          prev < suggestions.length - 1 ? prev + 1 : prev
        );
      }
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (showSuggestions && suggestions.length > 0) {
        setSelectedSuggestionIndex(prev => prev > 0 ? prev - 1 : -1);
      }
    } else if (e.key === 'Escape') {
      setShowSuggestions(false);
      setSelectedSuggestionIndex(-1);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setQuery(value);
    setDisplayQuery(value);
    setCompletion(null);
    setSelectedSuggestionIndex(-1);
  };

  const handleSuggestionSelect = (suggestion: string) => {
    setQuery(suggestion);
    setDisplayQuery(suggestion);
    setShowSuggestions(false);
    setCompletion(null);
    setSelectedSuggestionIndex(-1);
    handleSearch();
  };

  useEffect(() => {
    if (query) {
      handleSearch();
    }
  }, [currentFilter, currentPage]);

  useEffect(() => {
    const timeoutId = setTimeout(handleAutocomplete, 300);
    return () => clearTimeout(timeoutId);
  }, [query]);

  return (
    <div className="w-full max-w-4xl mx-auto">
      <div className="relative">
        <div className="relative group">
          <div className="absolute inset-0 bg-gradient-to-r from-teal-500 to-cyan-600 rounded-2xl blur opacity-25 group-hover:opacity-40 transition duration-200"></div>
          <div className="relative bg-white rounded-2xl shadow-xl border border-gray-100">
            <div className="flex items-center">
              <div className="pl-6">
                <Search className="text-gray-400 group-hover:text-teal-500 transition-colors duration-200" size={24} />
              </div>
              <div className="flex-1 relative">
                <input
                  ref={inputRef}
                  type="text"
                  value={query}
                  onChange={handleInputChange}
                  onKeyDown={handleKeyDown}
                  onFocus={() => setShowSuggestions(suggestions.length > 0)}
                  onBlur={() => setTimeout(() => setShowSuggestions(false), 150)}
                  placeholder="Rechercher des documents, images, vidéos..."
                  className="w-full p-6 bg-transparent text-lg placeholder-gray-400 focus:outline-none relative z-10"
                  style={{ backgroundColor: 'transparent' }}
                />
                {completion && (
                  <div className="absolute inset-0 p-6 text-lg pointer-events-none flex items-center">
                    <span className="invisible">{query}</span>
                    <span
                      ref={completionRef}
                      className="text-gray-400 bg-gray-100 pr-1 rounded"
                      style={{
                        backgroundColor: 'rgba(156, 163, 175, 0.2)',
                        color: '#9CA3AF'
                      }}
                    >
                      {isExtension ? completion : ' ' + completion}
                    </span>
                  </div>
                )}
              </div>
              {query && (
                <button
                  onClick={clearSearch}
                  className="mr-2 p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition-all duration-200"
                  title="Effacer"
                >
                  <X size={20} />
                </button>
              )}
              <button
                onClick={handleSearch}
                disabled={isLoading}
                className="mr-4 bg-gradient-to-r from-teal-500 to-cyan-600 text-white px-8 py-3 rounded-xl hover:from-teal-600 hover:to-cyan-700 transition-all duration-200 font-medium shadow-lg hover:shadow-xl disabled:opacity-50"
              >
                {isLoading ? (
                  <div className="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent"></div>
                ) : (
                  'Rechercher'
                )}
              </button>
            </div>
          </div>
        </div>
        {showSuggestions && suggestions.length > 0 && (
          <AutocompleteSuggestions
            suggestions={suggestions}
            onSelect={handleSuggestionSelect}
            onFill={handleSuggestionFill}
            selectedIndex={selectedSuggestionIndex}
          />
        )}
      </div>
    </div>
  );
};

export default SearchBar;