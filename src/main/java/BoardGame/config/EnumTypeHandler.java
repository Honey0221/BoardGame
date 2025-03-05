package BoardGame.config;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

// MyBatis에서 Enum 타입을 처리하기 위한 클래스
// 데이터베이스의 VARCHAR/ENUM 타입과 Java Enum 타입 간의 변환을 처리
public class EnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {
	private final Class<E> type;

	public EnumTypeHandler(Class<E> type) {
		this.type = type;
	}

	// Java Enum을 데이터베이스에 저장할 때 호출되는 메서드
	// Enum.name()을 사용하여 문자열로 변환
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, E parameter,
			JdbcType jdbcType) throws SQLException {
		ps.setString(i, parameter.name());
	}

	// 데이터베이스의 값을 Java Enum으로 변환할 때 호출되는 메서드 (컬럼명으로 조회)
	@Override
	public E getNullableResult(ResultSet rs, String columnName) throws
			SQLException {
		String value = rs.getString(columnName);
		return value == null ? null : Enum.valueOf(type, value);
	}

	// 데이터베이스의 값을 Java Enum으로 변환할 때 호출되는 메서드 (인덱스로 조회)
	@Override
	public E getNullableResult(ResultSet rs, int columnIndex) throws
			SQLException {
		String value = rs.getString(columnIndex);
		return value == null ? null : Enum.valueOf(type, value);
	}

	// 저장 프로시저의 결과를 Java Enum으로 변환할 때 호출되는 메서드
	@Override
	public E getNullableResult(CallableStatement cs, int columnIndex) throws
			SQLException {
		String value = cs.getString(columnIndex);
		return value == null ? null : Enum.valueOf(type, value);
	}
}
