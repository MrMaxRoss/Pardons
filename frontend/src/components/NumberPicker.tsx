interface Props {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
}

export default function NumberPicker({
  value,
  onChange,
  min = 1,
  max = 99,
}: Props) {
  return (
    <div className="flex items-center gap-3">
      <button
        type="button"
        onClick={() => onChange(Math.max(min, value - 1))}
        disabled={value <= min}
        className="w-10 h-10 rounded-full border border-gray-300 flex items-center justify-center text-xl font-bold text-gray-600 hover:bg-gray-100 disabled:opacity-30"
      >
        -
      </button>
      <span className="text-3xl font-bold text-indigo-600 w-12 text-center">
        {value}
      </span>
      <button
        type="button"
        onClick={() => onChange(Math.min(max, value + 1))}
        disabled={value >= max}
        className="w-10 h-10 rounded-full border border-gray-300 flex items-center justify-center text-xl font-bold text-gray-600 hover:bg-gray-100 disabled:opacity-30"
      >
        +
      </button>
    </div>
  );
}
