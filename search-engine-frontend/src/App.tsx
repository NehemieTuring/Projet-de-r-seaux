import SearchBar from './components/SearchBar';
import Filters from './components/Filters';
import SearchResults from './components/SearchResults';
import { useState, useEffect } from 'react';
import { checkHealth } from './services/api';
import type { HealthResponse } from './types/HealthResponse';
import type { SearchContext } from './types/SearchContext';
import { Search, MapPin, AlertCircle } from 'lucide-react';
import ico from './assets/ico.png'; 

function App() {
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [totalResults, setTotalResults] = useState(0);
  const [currentFilter, setCurrentFilter] = useState('ALL');
  const [currentPage, setCurrentPage] = useState(0);
  const [searchContext, setSearchContext] = useState<SearchContext | null>(null);
  const [healthStatus, setHealthStatus] = useState<string>('UP');

  // Check backend health periodically
  useEffect(() => {
    const checkBackendHealth = async () => {
      try {
        const response: HealthResponse = await checkHealth();
        setHealthStatus(response.status);
      } catch (error) {
        setHealthStatus('DOWN');
        console.error('Health check failed:', error);
      }
    };
    checkBackendHealth();
    const interval = setInterval(checkBackendHealth, 60000); // Check every 60s
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-teal-50 to-cyan-50">
      {/* Header */}
      <div className="relative">
        <div className="absolute inset-0 bg-gradient-to-r from-teal-900 to-cyan-600 opacity-10"></div>
        <div className="relative container mx-auto px-6 py-12">
          <div className="text-center mb-8">
            <div className="flex items-center justify-center gap-4 mb-6">
              <div className="w-30 h-30 bg-gradient-to-br from-teal-500 to-cyan-600 rounded-2xl flex items-center justify-center shadow-lg">
                <img src={ico} className="w-25 h-auto" alt="Icône" />
              </div>
              <div className="text-left">
                <h1 className="text-5xl font-bold bg-gradient-to-r from-teal-600 to-cyan-600 bg-clip-text text-transparent">
                  SearchFlow
                </h1>
                <p className="text-teal-600 font-medium text-lg">L'ESSENTIEL A PORTE DE MAIN</p>
              </div>
            </div>
            <p className="text-gray-600 text-lg max-w-2xl mx-auto">
              Recherchez parmi des millions de documents, images, vidéos et fichiers audio
            </p>
          </div>

          {/* Status indicators */}
          <div className="flex justify-center gap-4 mb-8">
            {healthStatus === 'DOWN' && (
              <div className="flex items-center gap-3 bg-red-50 text-red-700 px-6 py-3 rounded-xl border border-red-200 shadow-lg">
                <AlertCircle size={20} />
                <span className="font-medium">Service temporairement indisponible</span>
              </div>
            )}
            
            {searchContext?.geoLocation && (
              <div className="flex items-center gap-3 bg-teal-50 text-teal-700 px-6 py-3 rounded-xl border border-teal-200 shadow-lg">
                <MapPin size={20} />
                <span className="font-medium">
                  Résultats pour {searchContext.geoLocation.city || 'Ville inconnue'}, {searchContext.geoLocation.country || 'Pays inconnu'}
                </span>
              </div>
            )}
          </div>

          <SearchBar
            setResults={setSearchResults}
            setTotalResults={setTotalResults}
            setSearchContext={setSearchContext}
            currentFilter={currentFilter}
            currentPage={currentPage}
          />
        </div>
      </div>

      {/* Content */}
      <div className="container mx-auto px-6 pb-12">
        <Filters 
          currentFilter={currentFilter} 
          setFilter={setCurrentFilter} 
          setPage={setCurrentPage} 
        />
        
        <SearchResults 
          results={searchResults} 
          totalResults={totalResults} 
          currentPage={currentPage} 
          setPage={setCurrentPage} 
        />
      </div>
    </div>
  );
}

export default App;