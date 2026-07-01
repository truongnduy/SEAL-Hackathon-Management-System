import { Upload, message } from 'antd';
import { FilePdfOutlined } from '@ant-design/icons';

const ACCEPT = 'application/pdf,.pdf';
const MAX_BYTES = 25 * 1024 * 1024;

const isPdf = (file) =>
  file.type === 'application/pdf' || /\.pdf$/i.test(file.name || '');

const withPreview = (list = []) =>
  list.map((item) => {
    if (item.name || !item.originFileObj) {
      return item;
    }
    return { ...item, name: item.originFileObj.name };
  });

const RoundProblemPdfUpload = ({ value, fileList: fileListFromForm, onChange, disabled = false }) => {
  const fileList = withPreview(fileListFromForm ?? value ?? []);

  return (
    <Upload
      accept={ACCEPT}
      maxCount={1}
      disabled={disabled}
      fileList={fileList}
      beforeUpload={(file) => {
        if (!isPdf(file)) {
          message.error('Chỉ chấp nhận file PDF.');
          return Upload.LIST_IGNORE;
        }
        if (file.size > MAX_BYTES) {
          message.error('File đề bài tối đa 25MB.');
          return Upload.LIST_IGNORE;
        }
        return false;
      }}
      onChange={({ fileList: next }) => onChange?.(withPreview(next))}
      onRemove={() => onChange?.([])}
    >
      <ButtonLike disabled={disabled} hasFile={fileList.length > 0} />
    </Upload>
  );
};

const ButtonLike = ({ disabled, hasFile }) => (
  <div
    style={{
      border: '1px dashed #d9d9d9',
      borderRadius: 8,
      padding: '16px 20px',
      cursor: disabled ? 'not-allowed' : 'pointer',
      opacity: disabled ? 0.6 : 1,
      display: 'inline-flex',
      alignItems: 'center',
      gap: 10,
    }}
  >
    <FilePdfOutlined style={{ fontSize: 20, color: '#cf1322' }} />
    <span>{hasFile ? 'Đổi file PDF' : 'Chọn file PDF đề bài'}</span>
  </div>
);

export default RoundProblemPdfUpload;
