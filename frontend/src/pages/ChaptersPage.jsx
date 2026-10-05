import HelperPage from './HelperPage.jsx';
import { api } from '../api.js';
import { parseInteger, validateChapter } from '../validation.js';

const config = {
  entity: 'chapter',
  title: 'Ордены',
  newLabel: 'Новый орден',
  columns: [
    { title: 'Название', render: (c) => c.name },
    { title: 'Численность', render: (c) => c.marinesCount, num: true },
  ],
  fields: [
    { key: 'name', label: 'Название *' },
    { key: 'marinesCount', label: 'Численность *', hint: '1–1000', inputMode: 'numeric' },
  ],
  empty: { name: '', marinesCount: '' },
  fromItem: (c) => ({ name: c.name, marinesCount: String(c.marinesCount) }),
  validate: (f) => validateChapter(f.name, f.marinesCount),
  toBody: (f) => ({ name: f.name, marinesCount: parseInteger(f.marinesCount) }),
  label: (c) => `${c.name}`,
  api: api.chapters,
};

export default function ChaptersPage() {
  return <HelperPage config={config} />;
}
