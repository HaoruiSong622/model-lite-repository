package com.huawei.modellite.repository.infrastructure.persistence.typehandler;

import com.huawei.modellite.repository.common.enums.LockType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(LockType.class)
public class LockTypeTypeHandler extends BaseTypeHandler<LockType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LockType parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getDbValue());
    }

    @Override
    public LockType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : LockType.fromDbValue(value);
    }

    @Override
    public LockType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : LockType.fromDbValue(value);
    }

    @Override
    public LockType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : LockType.fromDbValue(value);
    }
}