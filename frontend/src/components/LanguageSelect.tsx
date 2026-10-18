import { Select } from 'antd';
import React from 'react';
import { changeLocale } from '@/core/i18n/apply';
import { useI18nState } from '@/core/i18n/store';

const LanguageSelect: React.FC = () => {
  const { locale, languages } = useI18nState();
  return (
    <Select
      aria-label="language"
      value={locale}
      style={{ minWidth: 140 }}
      options={languages.map((item) => ({
        value: item.supportLanguageCode,
        label: item.nativeName,
      }))}
      onChange={(value) => {
        void changeLocale(value);
      }}
    />
  );
};

export default LanguageSelect;
