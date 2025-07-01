import { ChevronLeft, ChevronRight, FileText, Image, Video, Music, Globe } from 'lucide-react';

interface SearchResultsProps {
  results: any[];
  totalResults: number;
  currentPage: number;
  setPage: (page: number) => void;
}

const SearchResults = ({ results, totalResults, currentPage, setPage }: SearchResultsProps) => {
  const resultsPerPage = 10;
  const totalPages = Math.ceil(totalResults / resultsPerPage);

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'PDF': return <FileText size={20} className="text-red-500" />;
      case 'IMAGE': return <Image size={20} className="text-green-500" />;
      case 'VIDEO': return <Video size={20} className="text-blue-500" />;
      case 'AUDIO': return <Music size={20} className="text-purple-500" />;
      default: return <Globe size={20} className="text-gray-500" />;
    }
  };

  return (
    <div className="w-full max-w-4xl mx-auto mt-8">
      {totalResults > 0 && (
        <div className="mb-6">
          <p className="text-gray-600 text-lg">
            <span className="font-semibold text-gray-800">{totalResults.toLocaleString()}</span> résultats trouvés
          </p>
        </div>
      )}
      
      <div className="space-y-6">
        {results.map((result) => (
          <div key={result._id || result.url } className="bg-white rounded-2xl p-6 shadow-lg border border-gray-100 hover:shadow-xl transition-all duration-200 group">
            <div className="flex items-start gap-4">
              <div className="flex-shrink-0 mt-1">
                {getTypeIcon(result.documentType)}
              </div>
              <div className="flex-1">
                <a 
                  href={result.url} 
                  target="_blank" 
                  rel="noopener noreferrer" 
                  className="block group-hover:translate-x-1 transition-transform duration-200"
                >
                  <h2 className="text-xl font-semibold text-gray-900 mb-2 group-hover:text-teal-600 transition-colors duration-200">
                    {result.title}
                  </h2>
                  <p className="text-green-600 text-sm mb-3 font-medium">
                    {result.url}
                  </p>
                  <p className="text-gray-700 leading-relaxed">
                    {result.snippet || result.description || 'Aucun aperçu disponible'}
                  </p>
                </a>
              </div>
            </div>
          </div>
        ))}
      </div>

      {totalResults > resultsPerPage && (
        <div className="flex justify-center items-center gap-4 mt-12">
          <button
            onClick={() => setPage(currentPage - 1)}
            disabled={currentPage === 0}
            className="flex items-center gap-2 px-6 py-3 bg-white rounded-xl shadow-lg border border-gray-200 disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-xl transition-all duration-200"
          >
            <ChevronLeft size={20} />
            Précédent
          </button>
          
          <div className="flex items-center gap-2">
            <span className="text-gray-600">
              Page {currentPage + 1} sur {totalPages}
            </span>
          </div>
          
          <button
            onClick={() => setPage(currentPage + 1)}
            disabled={currentPage >= totalPages - 1}
            className="flex items-center gap-2 px-6 py-3 bg-white rounded-xl shadow-lg border border-gray-200 disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-xl transition-all duration-200"
          >
            Suivant
            <ChevronRight size={20} />
          </button>
        </div>
      )}
    </div>
  );
};

export default SearchResults;