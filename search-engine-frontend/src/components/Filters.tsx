import { Filter, Globe, FileText, Image, Video, Music } from 'lucide-react';

interface FiltersProps {
  currentFilter: string;
  setFilter: (filter: string) => void;
  setPage: (page: number) => void;
}

const Filters = ({ currentFilter, setFilter, setPage }: FiltersProps) => {
  const filters = [
    { key: 'ALL', label: 'Tout', icon: Globe, color: 'text-gray-600', documentType: undefined },
    { key: 'PDF', label: 'Documents', icon: FileText, color: 'text-red-500', documentType: 'pdf' },
    { key: 'IMAGE', label: 'Images', icon: Image, color: 'text-green-500', documentType: 'image' },
    { key: 'VIDEO', label: 'Vidéos', icon: Video, color: 'text-blue-500', documentType: 'video' },
    { key: 'WEB_PAGE', label: 'Pages Web', icon: Globe, color: 'text-blue-600', documentType: 'web_page' }
  ];

  return (
    <div className="w-full mb-8">
      <div className="bg-white rounded-2xl shadow-lg border border-gray-100 p-6">
        <div className="flex items-center gap-3 mb-6">
          <Filter className="text-teal-600" size={24} />
          <h3 className="text-xl font-semibold text-gray-800">
            Filtrer par type
          </h3>
        </div>
        
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
          {filters.map((filter) => {
            const IconComponent = filter.icon;
            const isActive = currentFilter === filter.key;
            
            return (
              <button
                key={filter.key}
                onClick={() => {
                  setFilter(filter.key);
                  setPage(0);
                }}
                className={`group relative flex flex-col items-center gap-3 p-4 rounded-xl font-medium transition-all duration-300 ${
                  isActive
                    ? 'bg-gradient-to-br from-teal-50 to-cyan-50 border-2 border-teal-200 shadow-lg'
                    : 'bg-gray-50 hover:bg-gray-100 border border-gray-200 hover:shadow-md'
                }`}
              >
                <IconComponent 
                  size={28} 
                  className={`${
                    isActive 
                      ? 'text-teal-600' 
                      : `${filter.color} group-hover:text-teal-500`
                  } transition-colors duration-300`} 
                />
                <span className={`text-sm ${
                  isActive 
                    ? 'text-teal-700 font-semibold' 
                    : 'text-gray-700 group-hover:text-teal-600'
                } transition-colors duration-300`}>
                  {filter.label}
                </span>
                {isActive && (
                  <div className="absolute inset-0 rounded-xl bg-teal-100 opacity-20 animate-pulse" />
                )}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default Filters;