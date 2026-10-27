import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { getSellerProducts, stopProduct } from '../api/productApi';
import type { SellerProduct } from '../types/product';
import StopProductDialog from '../components/StopProductDialog';

function SellerProductListPage() {
    const [products, setProducts] = useState<SellerProduct[]>([]);
    const [includeStopped, setIncludeStopped] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [reload, setReload] = useState(0);
    const [selected, setSelected] = useState<SellerProduct | null>(null);
    const [stopping, setStopping] = useState(false);
    const [stopError, setStopError] = useState('');

    useEffect(() => {
        let active = true;
        setLoading(true);
        setError('');
        getSellerProducts(includeStopped).then(data => {
            if (active) setProducts(data);
        }).catch(() => {
            if (active) setError('판매상품 목록을 불러오지 못했습니다.');
        }).finally(() => {
            if (active) setLoading(false);
        });
        return () => { active = false; };
    }, [includeStopped, reload]);

    const handleStop = async () => {
        if (!selected || stopping) return;
        setStopping(true);
        setStopError('');
        try {
            await stopProduct(selected.productId);
            setProducts(current => includeStopped
                ? current.map(product => product.productId === selected.productId
                    ? { ...product, deleted: true, productStatus: 'STOPPED' } : product)
                : current.filter(product => product.productId !== selected.productId));
            setSelected(null);
        } catch {
            setStopError('판매중지에 실패했습니다. 다시 시도해주세요.');
        } finally {
            setStopping(false);
        }
    };

    return <main>
        <h1>내 판매상품</h1>
        <Link to="/seller/products/new">상품 등록</Link>
        <p><label><input type="checkbox" checked={includeStopped} disabled={selected !== null}
            onChange={event => setIncludeStopped(event.target.checked)} /> 판매중지된 상품 보기</label></p>
        {loading ? <p>상품을 불러오는 중입니다.</p> : error
            ? <p role="alert">{error} <button onClick={() => setReload(value => value + 1)}>다시 시도</button></p>
            : products.length === 0 ? <p>등록된 상품이 없습니다.</p>
                : <ul>{products.map(product => <li key={product.productId}>
                    <h2>{product.productName}</h2>
                    <p>가격: {product.price.toLocaleString()}원</p>
                    <p>재고: {product.stock}개</p>
                    <p>상태: {product.deleted || product.productStatus === 'STOPPED' ? '판매중지' : product.productStatus === 'SOLD_OUT' ? '품절' : '판매중'}</p>
                    <Link to={`/seller/products/${product.productId}/edit`}>상품수정</Link>{' '}
                    <button disabled={product.deleted || selected !== null} onClick={() => {
                        setStopError(''); setSelected(product);
                    }}>판매중지</button>
                </li>)}</ul>}
        {selected && <StopProductDialog productName={selected.productName} pending={stopping} error={stopError}
            onConfirm={() => void handleStop()} onCancel={() => setSelected(null)} />}
    </main>;
}

export default SellerProductListPage;
