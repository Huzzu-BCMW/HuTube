import React, { useRef } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import MediaCard from './MediaCard';

export default function CategoryRow({ title, icon, items = [], onPlay, onOpenShow }) {
  const rowRef = useRef(null);

  if (!items || items.length === 0) return null;

  const scroll = (direction) => {
    if (rowRef.current) {
      const { scrollLeft, clientWidth } = rowRef.current;
      const scrollAmount = clientWidth * 0.75;
      rowRef.current.scrollTo({
        left: direction === 'left' ? scrollLeft - scrollAmount : scrollLeft + scrollAmount,
        behavior: 'smooth'
      });
    }
  };

  return (
    <section className="relative my-6 px-4 md:px-10 group/row">
      {/* Category Header */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2.5">
          {icon && <span className="text-xl">{icon}</span>}
          <h2 className="text-lg md:text-xl font-bold tracking-tight text-white group-hover/row:text-red-400 transition-colors">
            {title}
          </h2>
          <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-neutral-800/80 text-neutral-400">
            {items.length}
          </span>
        </div>
      </div>

      {/* Horizontal Carousel */}
      <div className="relative">
        {/* Left Arrow */}
        <button
          onClick={() => scroll('left')}
          className="absolute left-0 top-1/2 -translate-y-1/2 -ml-3 md:-ml-5 z-20 w-8 md:w-10 h-16 md:h-24 bg-black/70 hover:bg-brand-red text-white flex items-center justify-center rounded-r-lg opacity-0 group-hover/row:opacity-100 transition-all backdrop-blur-sm shadow-xl"
        >
          <ChevronLeft className="w-6 h-6" />
        </button>

        {/* Card Scroll Track */}
        <div
          ref={rowRef}
          className="flex items-center gap-4 overflow-x-auto no-scrollbar scroll-smooth py-2 px-1"
        >
          {items.map((item) => (
            <MediaCard
              key={item.id}
              item={item}
              onPlay={onPlay}
              onOpenShow={onOpenShow}
            />
          ))}
        </div>

        {/* Right Arrow */}
        <button
          onClick={() => scroll('right')}
          className="absolute right-0 top-1/2 -translate-y-1/2 -mr-3 md:-mr-5 z-20 w-8 md:w-10 h-16 md:h-24 bg-black/70 hover:bg-brand-red text-white flex items-center justify-center rounded-l-lg opacity-0 group-hover/row:opacity-100 transition-all backdrop-blur-sm shadow-xl"
        >
          <ChevronRight className="w-6 h-6" />
        </button>
      </div>
    </section>
  );
}
