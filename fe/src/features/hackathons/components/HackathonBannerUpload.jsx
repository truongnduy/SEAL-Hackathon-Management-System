import { Upload, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';

const ACCEPT = 'image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp';
const MAX_BYTES = 5 * 1024 * 1024;

const isAllowedImage = (file) => {
  if (file.type?.startsWith('image/')) return true;
  return /\.(jpe?g|png|webp)$/i.test(file.name || '');
};

const withPreview = (list = []) =>
  list.map((item) => {
    if (item.url || item.thumbUrl || !item.originFileObj) {
      return item;
    }
    return {
      ...item,
      thumbUrl: URL.createObjectURL(item.originFileObj),
    };
  });

const HackathonBannerUpload = ({ value, fileList: fileListFromForm, onChange, disabled = false }) => {
  const fileList = withPreview(fileListFromForm ?? value ?? []);

  const handleChange = ({ fileList: next }) => {
    onChange?.(withPreview(next));
  };

  return (
    <Upload
      listType="picture-card"
      fileList={fileList}
      maxCount={1}
      disabled={disabled}
      accept={ACCEPT}
      beforeUpload={(file) => {
        if (!isAllowedImage(file)) {
          message.error('Chỉ chấp nhận ảnh JPG, PNG hoặc WebP.');
          return Upload.LIST_IGNORE;
        }
        if (file.size > MAX_BYTES) {
          message.error('Ảnh banner tối đa 5MB.');
          return Upload.LIST_IGNORE;
        }
        return false;
      }}
      onChange={handleChange}
      onRemove={() => onChange?.([])}
    >
      {fileList.length >= 1 ? null : (
        <div>
          <PlusOutlined />
          <div style={{ marginTop: 8 }}>Tải ảnh lên</div>
        </div>
      )}
    </Upload>
  );
};

export default HackathonBannerUpload;
