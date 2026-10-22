import { useIntl } from '@umijs/max';
import { Form, Input } from 'antd';
import React from 'react';
import type { SupportLanguage } from '@/services/mdm/data';

type Props = {
  languages: SupportLanguage[];
};

const NameTranslations: React.FC<Props> = ({ languages }) => {
  const intl = useIntl();
  const nameLabel = intl.formatMessage({ id: 'pages.org.name', defaultMessage: '이름' });
  return (
    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
      <tbody>
        {languages.map((language) => (
          <tr key={language.supportLanguageCode}>
            <td style={{ padding: '8px 12px 8px 0', whiteSpace: 'nowrap' }}>{language.languageName}</td>
            <td style={{ padding: '8px 0' }}>
              <Form.Item name={['names', language.supportLanguageCode]} noStyle>
                <Input maxLength={2000} aria-label={`${language.languageName} ${nameLabel}`} />
              </Form.Item>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};

export default NameTranslations;
